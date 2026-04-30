package com.learn.bankingapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class PinRateLimitServiceTest {

    private PinRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new PinRateLimitService();
    }

    @Test
    void check_NoAttempts_Success() {
        assertDoesNotThrow(() -> rateLimitService.check(1L, 1L));
    }

    @Test
    void registerFailure_IncrementsCount() {
        for (int i = 0; i < 4; i++) {
            rateLimitService.registerFailure(1L, 1L);
            assertDoesNotThrow(() -> rateLimitService.check(1L, 1L));
        }

        rateLimitService.registerFailure(1L, 1L);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> rateLimitService.check(1L, 1L));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }

    @Test
    void reset_ClearsAttempts() {
        for (int i = 0; i < 4; i++) {
            rateLimitService.registerFailure(1L, 1L);
        }
        rateLimitService.reset(1L, 1L);
        assertDoesNotThrow(() -> rateLimitService.check(1L, 1L));
    }
}
