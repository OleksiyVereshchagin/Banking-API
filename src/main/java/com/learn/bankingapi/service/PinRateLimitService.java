package com.learn.bankingapi.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class PinRateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = 2 * 60 * 1000; // 2 хвилини

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class AttemptInfo {
        int count;
        long blockUntil;
    }

    private String key(long userId, long cardId) {
        return userId + ":" + cardId;
    }

    // перевірка перед операцією
    public void check(long userId, long cardId) {

        AttemptInfo info = attempts.get(key(userId, cardId));

        if (info != null) {

            // ⛔ ще заблокований
            if (System.currentTimeMillis() < info.blockUntil) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "PIN temporarily blocked"
                );
            }

            // 🧹 блок вже минув → очищаємо стан
            if (info.blockUntil != 0) {
                attempts.remove(key(userId, cardId));
            }
        }
    }

    // викликається при невдалій спробі
    public void registerFailure(long userId, long cardId) {

        String key = key(userId, cardId);

        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());

        info.count++;

        if (info.count >= MAX_ATTEMPTS) {
            info.blockUntil = System.currentTimeMillis() + BLOCK_TIME_MS;
            info.count = 0; // скидаємо лічильник після блокування
        }
    }

    // викликається при успіху
    public void reset(long userId, long cardId) {
        attempts.remove(key(userId, cardId));
    }
}