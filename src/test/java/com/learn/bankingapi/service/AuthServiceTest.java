package com.learn.bankingapi.service;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.dto.request.auth.LoginRequest;
import com.learn.bankingapi.dto.request.auth.RegisterRequest;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.entity.RefreshToken;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.repository.RefreshTokenRepository;
import com.learn.bankingapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);

        registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "Smith",
                LocalDate.of(1990, 1, 1)
        );
    }

    @Test
    void registration_Success() {
        when(userRepository.existsUserByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("accessToken");

        AuthResponse response = authService.registration(registerRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.token());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registration_Conflict_UserExists() {
        when(userRepository.existsUserByEmailIgnoreCase("test@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.registration(registerRequest));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken");

        AuthLoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.token());
        assertNotNull(response.refreshToken());
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_Success() {
        String requestToken = "validRefreshToken";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(requestToken);
        refreshToken.setUser(testUser);
        refreshToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

        when(refreshTokenRepository.findByToken(requestToken)).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateToken(testUser)).thenReturn("newAccessToken");

        AuthResponse response = authService.refreshToken(requestToken);

        assertNotNull(response);
        assertEquals("newAccessToken", response.token());
    }

    @Test
    void refreshToken_Forbidden_TokenNotFound() {
        String requestToken = "nonExistentToken";
        when(refreshTokenRepository.findByToken(requestToken)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(requestToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Refresh token is not in database", exception.getReason());
    }

    @Test
    void refreshToken_Forbidden_TokenExpired() {
        String requestToken = "expiredToken";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(requestToken);
        refreshToken.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));

        when(refreshTokenRepository.findByToken(requestToken)).thenReturn(Optional.of(refreshToken));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(requestToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("expired"));
        verify(refreshTokenRepository).delete(refreshToken);
    }
}
