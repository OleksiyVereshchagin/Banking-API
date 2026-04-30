package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", secret);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", expiration);
    }

    @Test
    void generateToken_Success() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        String userId = jwtService.extractUserId(token);
        assertEquals("1", userId);
    }

    @Test
    void isTokenValid_Success() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);
        String token = jwtService.generateToken(user);

        boolean isValid = jwtService.isTokenValid(token, "1");

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_InvalidUserId() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);
        String token = jwtService.generateToken(user);

        boolean isValid = jwtService.isTokenValid(token, "2");

        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_False() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenExpired(token));
    }
}
