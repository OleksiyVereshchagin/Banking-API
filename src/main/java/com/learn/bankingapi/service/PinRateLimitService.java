package com.learn.bankingapi.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing rate limits on PIN-related operations.
 * It tracks failed attempts and enforces temporary blocking to prevent brute-force attacks.
 * Data is stored in-memory using a thread-safe map.
 */
@Service
public class PinRateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = 2 * 60 * 1000; // 2 minutes

    /**
     * Internal storage for tracking attempts per user-card pair.
     * Key format: "userId:cardId"
     */
    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class AttemptInfo {
        volatile int count;
        volatile long blockUntil;
    }

    /**
     * Generates a unique key for tracking attempts based on user and card identifiers.
     */
    private String generateKey(long userId, long cardId) {
        return userId + ":" + cardId;
    }


    /**
     * Validates if the operation can proceed based on the current rate limit state.
     *
     * Logic flow:
     * 1. If a block is active (currentTime < blockUntil), throws 429 TOO_MANY_REQUESTS.
     * 2. If a previously set block has expired, it performs a "fresh start" by
     *    removing the record from the map to reset the counter for the next cycle.
     *
     * @param userId The ID of the user performing the action.
     * @param cardId The ID of the card being accessed.
     * @throws ResponseStatusException with 429 TOO_MANY_REQUESTS if the PIN is temporarily blocked.
     */
    public void check(long userId, long cardId) {
        AttemptInfo info = attempts.get(generateKey(userId, cardId));

        if (info != null) {
            // Check if the block period is still active
            if (System.currentTimeMillis() < info.blockUntil) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "PIN temporarily blocked");
            }

            // "Fresh Start" logic: If the block has expired, clear the state.
            // This allows the user to start a new sequence of 5 attempts.
            if (info.blockUntil != 0) {
                attempts.remove(generateKey(userId, cardId));
            }
        }
    }

    /**
     * Increments the failure counter for a specific user-card combination.
     * When MAX_ATTEMPTS (5) is reached, it sets a block timestamp for 2 minutes.
     *
     * Note: We don't need to manually reset info.Count here because the record
     * will be removed from the map during the next check() call after the block expires,
     * or via reset() on success, ensuring a fresh start.
     *
     * @param userId The ID of the user who failed the PIN check.
     * @param cardId The ID of the card for which the PIN was incorrect.
     */
    public void registerFailure(long userId, long cardId) {

        String key = generateKey(userId, cardId);

        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());

        info.count++;

        if (info.count >= MAX_ATTEMPTS) {
            info.blockUntil = System.currentTimeMillis() + BLOCK_TIME_MS;
        }
    }

    /**
     * Completely resets the rate limit data for a specific user-card pair.
     * This method should be called only after successful PIN verification.
     * It ensures that any previous failed attempts (below the threshold) are cleared.
     *
     * @param userId The ID of the user.
     * @param cardId The ID of the card.
     */
    public void reset(long userId, long cardId) {
        attempts.remove(generateKey(userId, cardId));
    }
}