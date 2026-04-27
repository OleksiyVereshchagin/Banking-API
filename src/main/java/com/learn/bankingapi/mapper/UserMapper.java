package com.learn.bankingapi.mapper;

import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.response.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toDto(User user){
        return new UserResponse(
                user.getPhoneNumber(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                user.getDateOfBirth(),
                user.getRole()
        );
    }
}
