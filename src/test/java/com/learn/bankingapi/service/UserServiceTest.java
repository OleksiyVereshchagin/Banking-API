package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.user.UpdateUserPassword;
import com.learn.bankingapi.dto.request.user.UpdateUserProfile;
import com.learn.bankingapi.dto.response.user.UserResponse;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.UserMapper;
import com.learn.bankingapi.repository.UserRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private CurrentUserProvider userProvider;

    @Spy
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMiddleName("Smith");
        testUser.setPhoneNumber("1234567890");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setRole(UserRole.USER);
    }

    @Test
    void getUserProfile_Success() {
        when(userProvider.getCurrentUser()).thenReturn(testUser);

        UserResponse response = userService.getUserProfile();

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.email());
        assertEquals(testUser.getFirstName(), response.firstName());
        verify(userProvider).getCurrentUser();
        verify(userMapper).toDto(testUser);
    }

    @Test
    void updateUserProfile_Success() {
        UpdateUserProfile dto = new UpdateUserProfile("Jane", "Smith", null, null);
        when(userProvider.getCurrentUser()).thenReturn(testUser);

        UserResponse response = userService.updateUserProfile(dto);

        assertNotNull(response);
        assertEquals("Jane", testUser.getFirstName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("Smith", testUser.getMiddleName()); // remained unchanged
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_BadRequest_NoFields() {
        UpdateUserProfile dto = new UpdateUserProfile(null, null, null, null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> userService.updateUserProfile(dto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("At least one field must be provided for update", exception.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserPassword_Success() {
        UpdateUserPassword dto = new UpdateUserPassword("oldPassword", "newPassword");
        when(userProvider.getCurrentUser()).thenReturn(testUser);
        when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        userService.updateUserPassword(dto);

        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserPassword_BadRequest_SamePassword() {
        UpdateUserPassword dto = new UpdateUserPassword("oldPassword", "oldPassword");
        when(userProvider.getCurrentUser()).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> userService.updateUserPassword(dto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("New password must be different from current password", exception.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserPassword_BadRequest_InvalidCurrentPassword() {
        UpdateUserPassword dto = new UpdateUserPassword("wrongPassword", "newPassword");
        when(userProvider.getCurrentUser()).thenReturn(testUser);
        when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> userService.updateUserPassword(dto));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid current password", exception.getReason());
        verify(userRepository, never()).save(any());
    }
}
