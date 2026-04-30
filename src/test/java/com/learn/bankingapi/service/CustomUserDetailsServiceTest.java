package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_EmailSuccess() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        user.setPassword("password");

        when(userRepository.findUserByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_PhoneSuccess() {
        User user = new User();
        user.setPhoneNumber("123456789");
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        user.setPassword("password");

        when(userRepository.findUserByEmailIgnoreCase("123456789")).thenReturn(Optional.empty());
        when(userRepository.findUserByPhoneNumber("123456789")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("123456789");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_NotFound() {
        when(userRepository.findUserByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findUserByPhoneNumber("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("unknown"));
    }
}
