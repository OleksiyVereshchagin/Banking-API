package com.learn.bankingapi.controller.openapi;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.user.UpdateUserPassword;
import com.learn.bankingapi.dto.request.user.UpdateUserProfile;
import com.learn.bankingapi.dto.response.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User Controller", description = "User profile management")
public interface UserApi {

    @Operation(summary = "Get user profile", description = "Returns data of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile successfully retrieved")
    @ApiResponse(responseCode = "401", description = "User is not authorized",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"User is not authorized\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    UserResponse getUserProfile();

    @Operation(summary = "Update user profile", description = "Allows changing first name, last name, middle name, and date of birth")
    @ApiResponse(responseCode = "200", description = "Profile successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Invalid request body\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "401", description = "User is not authorized",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"Invalid credentials\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "422", description = "Validation error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 422, \"message\": \"firstName: Size must be between 0 and 40\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    UserResponse updateUserProfile(@Valid @RequestBody UpdateUserProfile updateUserProfile);

    @Operation(summary = "Change password", description = "Allows the user to change their current password to a new one")
    @ApiResponse(responseCode = "204", description = "Password successfully changed")
    @ApiResponse(responseCode = "400", description = "Invalid current password or incorrect data",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Invalid current password\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "401", description = "User is not authorized",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"Invalid credentials\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "422", description = "Validation error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 422, \"message\": \"newPassword: size must be between 8 and 2147483647\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    void updatePassword(@Valid @RequestBody UpdateUserPassword updateUserPassword);
}