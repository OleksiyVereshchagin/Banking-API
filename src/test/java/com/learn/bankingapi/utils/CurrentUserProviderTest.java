package com.learn.bankingapi.utils;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserProviderTest {

    private CurrentUserProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CurrentUserProvider();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUser()).thenReturn(user);
        
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = provider.getCurrentUser();

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void getCurrentUser_NotAuthenticated_ThrowsException() {
        SecurityContextHolder.getContext().setAuthentication(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> provider.getCurrentUser());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User is not authenticated or session has expired", exception.getReason());
    }

    @Test
    void getCurrentUser_WrongPrincipalType_ThrowsException() {
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken("wrongPrincipal", null);
        
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> provider.getCurrentUser());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
