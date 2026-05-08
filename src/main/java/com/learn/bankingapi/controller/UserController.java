package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.openapi.UserApi;
import com.learn.bankingapi.dto.request.user.UpdateUserPassword;
import com.learn.bankingapi.dto.request.user.UpdateUserProfile;
import com.learn.bankingapi.dto.response.user.UserResponse;
import com.learn.bankingapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController implements UserApi {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Override
    public UserResponse getUserProfile() {
        return userService.getUserProfile();
    }

    @PatchMapping("/profile")
    @Override
    public UserResponse updateUserProfile(@Valid @RequestBody UpdateUserProfile updateUserProfile) {
        return userService.updateUserProfile(updateUserProfile);
    }

    @PatchMapping("/profile/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void updatePassword(@Valid @RequestBody UpdateUserPassword updateUserPassword) {
        userService.updateUserPassword(updateUserPassword);
    }
}