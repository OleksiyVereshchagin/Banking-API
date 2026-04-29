package com.learn.bankingapi.controller.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ExceptionResponse> handleResponseStatusException (ResponseStatusException exception){
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        ExceptionResponse responseBody = ExceptionResponse.error(exception.getStatusCode().value(),
                exception.getReason());

        return new ResponseEntity<>(responseBody, buildHeaders(), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Unexpected error"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");

        ExceptionResponse response = ExceptionResponse.error(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                message);

        return new ResponseEntity<>(response, buildHeaders(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler({
            org.springframework.security.authentication.BadCredentialsException.class,
            org.springframework.security.core.userdetails.UsernameNotFoundException.class
    })
    public ResponseEntity<ExceptionResponse> handleAuthErrors(Exception ex) {

        ExceptionResponse response = ExceptionResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid credentials");

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ExceptionResponse> handleIllegalArgument(Exception ex) {
        return new ResponseEntity<>(
                ExceptionResponse.error(400, "Invalid request value"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        return new ResponseEntity<>(
                ExceptionResponse.error(400, "Invalid request value"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleNotReadable(HttpMessageNotReadableException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ExceptionResponse.error(
                        400,
                        "Invalid request body"
                ));
    }

    @ExceptionHandler({
            io.jsonwebtoken.JwtException.class,
            io.jsonwebtoken.MalformedJwtException.class
    })
    public ResponseEntity<ExceptionResponse> handleJwtErrors(Exception ex) {
        return new ResponseEntity<>(
                ExceptionResponse.error(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Invalid token"
                ),
                HttpStatus.UNAUTHORIZED
        );
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
