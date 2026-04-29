package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.user.UpdateUserPassword;
import com.learn.bankingapi.dto.request.user.UpdateUserProfile;
import com.learn.bankingapi.dto.response.user.UserResponse;
import com.learn.bankingapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "User profile management")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Returns data of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile successfully retrieved")
    @ApiResponse(responseCode = "401", description = "User is not authorized", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    public UserResponse getUserProfile(){
        return userService.getUserProfile();
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update user profile", description = "Allows changing first name, last name, middle name, and date of birth")
    @ApiResponse(responseCode = "200", description = "Profile successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    @ApiResponse(responseCode = "401", description = "User is not authorized", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    @ApiResponse(responseCode = "422", description = "Validation error", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    public UserResponse updateUserProfile(@Valid @RequestBody UpdateUserProfile updateUserProfile){
        return userService.updateUserProfile(updateUserProfile);
    }

    @PatchMapping("/profile/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password", description = "Allows the user to change their current password to a new one")
    @ApiResponse(responseCode = "204", description = "Password successfully changed")
    @ApiResponse(responseCode = "400", description = "Invalid current password or incorrect data", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    @ApiResponse(responseCode = "401", description = "User is not authorized", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    @ApiResponse(responseCode = "422", description = "Validation error", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    public void updatePassword(@Valid @RequestBody UpdateUserPassword updateUserPassword){
        userService.updateUserPassword(updateUserPassword);
    }
}
