package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.request.user.UpdateUserPassword;
import com.learn.bankingapi.dto.request.user.UpdateUserProfile;
import com.learn.bankingapi.dto.response.user.UserResponse;
import com.learn.bankingapi.mapper.UserMapper;
import com.learn.bankingapi.repository.UserRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {
    private final CurrentUserProvider userProvider;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(CurrentUserProvider userProvider, UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userProvider = userProvider;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves the profile details of the currently authenticated user.
     *
     * @return A {@code UserResponse} object containing the user's profile information.
     * @throws ResponseStatusException with 401 UNAUTHORIZED if the user is not authenticated
     */
    public UserResponse getUserProfile(){
        User user = userProvider.getCurrentUser();
        return userMapper.toDto(user);
    }

    /**
     * Updates the user profile with the provided fields.
     *
     * @param dto The request object containing updated profile fields.
     * @return A {@code UserResponse} object representing the updated profile.
     * @throws ResponseStatusException with:
     *         - 400 BAD_REQUEST if all fields in the DTO are null (at least one field is required)
     *         - 401 UNAUTHORIZED if the user is not authenticated
     */
    public UserResponse updateUserProfile(UpdateUserProfile dto) {
        validateAtLeastOneFieldPresent(dto);
        User user = userProvider.getCurrentUser();

        applyDtoToUser(dto, user);

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    /**
     * Changes the user's password after verifying the current password and ensuring uniqueness.
     *
     * @param dto The request object containing the current and new passwords.
     * @throws ResponseStatusException with:
     *         - 400 BAD_REQUEST if:
     *           - the new password is the same as the current password
     *           - the provided current password does not match the stored password
     *         - 401 UNAUTHORIZED if the user is not authenticated
     */
    public void updateUserPassword(UpdateUserPassword dto) {
        User user = userProvider.getCurrentUser();

        // 1. Check if the new password is not the same as the old one (security best practice)
        if (passwordEncoder.matches(dto.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        // 2. Verify current password
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    // Helper Methods

    private void validateAtLeastOneFieldPresent(UpdateUserProfile dto) {
        if (dto.firstName() == null && dto.lastName() == null &&
                dto.middleName() == null && dto.dateOfBirth() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided for update");
        }
    }

    private void applyDtoToUser(UpdateUserProfile dto, User user) {
        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.middleName() != null) user.setMiddleName(dto.middleName());
        if (dto.dateOfBirth() != null) user.setDateOfBirth(dto.dateOfBirth());
    }

}
