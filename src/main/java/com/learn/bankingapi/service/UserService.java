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

    @Autowired
    public UserService(CurrentUserProvider userProvider, UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userProvider = userProvider;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getUserProfile(){
        User user = userProvider.getCurrentUser();
        return userMapper.toDto(user);
    }


    public UserResponse updateUserProfile(UpdateUserProfile dto) {

        if (
                dto.firstName() == null &&
                        dto.lastName() == null &&
                        dto.middleName() == null &&
                        dto.dateOfBirth() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    " At least one field must be provided"
            );
        }

        User user = userProvider.getCurrentUser();

        if (dto.firstName() != null) {
            if (dto.firstName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "First name cannot be blank"
                );
            }
            user.setFirstName(dto.firstName());
        }

        if (dto.lastName() != null) {
            if (dto.lastName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Last name cannot be blank"
                );
            }
            user.setLastName(dto.lastName());
        }

        if (dto.middleName() != null) {
            user.setMiddleName(dto.middleName());
        }

        if (dto.dateOfBirth() != null) {
            user.setDateOfBirth(dto.dateOfBirth());
        }

        userRepository.save(user);

        return userMapper.toDto(user);
    }

    public void updateUserPassword(UpdateUserPassword updateUserPassword){
        User user = userProvider.getCurrentUser();

        if(updateUserPassword.currentPassword().equals(updateUserPassword.newPassword())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different from current password"
            );
        }

        if(!passwordEncoder.matches(updateUserPassword.currentPassword(), user.getPassword())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid current password"
            );
        }

        user.setPassword(passwordEncoder.encode(updateUserPassword.newPassword()));
        userRepository.save(user);
    }

    public void updateUserFromDto(UpdateUserProfile dto, User user){
        if(dto.firstName() != null){
            user.setFirstName(dto.firstName());
        }
        if(dto.lastName() != null){
            user.setLastName(dto.lastName());
        }
        if(dto.middleName() != null){
            user.setMiddleName(dto.middleName());
        }
        if(dto.dateOfBirth() != null){
            user.setDateOfBirth(dto.dateOfBirth());
        }
    }
}
