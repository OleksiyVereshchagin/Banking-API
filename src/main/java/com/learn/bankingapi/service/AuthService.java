package com.learn.bankingapi.service;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.dto.response.auth.RegistrationResponse;
import com.learn.bankingapi.entity.RefreshToken;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.dto.request.auth.LoginRequest;
import com.learn.bankingapi.dto.request.auth.RegisterRequest;
import com.learn.bankingapi.enums.VerificationType;
import com.learn.bankingapi.repository.RefreshTokenRepository;
import com.learn.bankingapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationService verificationService;

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository, VerificationService verificationService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationService = verificationService;
    }

    /**
     * Registers a new user based on the provided registration details.
     *
     * @param request The registration request containing user details.
     * @return An authentication response containing a generated token for the newly registered user.
     * @throws ResponseStatusException with:
     *         - 409 CONFLICT if a user with the given login (email or phone) already exists
     */
    public RegistrationResponse registration(RegisterRequest request) {
        String login = request.login();

        if (isUserExists(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with login " + login + " already exists");
        }

        User user = createNewUser(request);
        userRepository.save(user);

        String message;
        if (login.contains("@")) {
            message = "Verification code has been sent to your email address";
            verificationService.sendVerificationCode(user, login, VerificationType.EMAIL);
        } else {
            message = "Verification code has been sent via text message to your phone number";}

        return new RegistrationResponse(message);
    }


    /**
     * Authenticates a user based on the provided credentials and generates an access token 
     * and a refresh token for the session.
     *
     * @param request the {@code LoginRequest} containing the user's login credentials,
     *                including the login (username or email) and password.
     * @return an {@code AuthLoginResponse} containing the access token and refresh token
     *         for the authenticated user.
     * @throws org.springframework.security.core.AuthenticationException if authentication fails
     *         (invalid credentials, locked account, etc.), typically resulting in a 401 UNAUTHORIZED response
     */
    public AuthLoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        
        // Очищення старих сесій та примусовий flush для уникнення порушення unique constraint
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = createRefreshToken(user);
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateToken(user);
        return new AuthLoginResponse(accessToken, refreshToken.getToken());
    }
    
    /**
     * Refreshes the access token based on a valid refresh token.
     *
     * @param requestToken the refresh token provided by the client
     * @return an {@link AuthResponse} containing the new access token
     * @throws ResponseStatusException with:
     *         - 403 FORBIDDEN if the refresh token is:
     *           - not found in the database
     *           - expired (in this case, the token is also deleted from the database)
     */
    public AuthResponse refreshToken(String requestToken) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(requestToken);

        if (tokenOptional.isPresent()) {
            RefreshToken token = tokenOptional.get();
            verifyExpiration(token);

            User user = token.getUser();
            String accessToken = jwtService.generateToken(user);

            return new AuthResponse(accessToken);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is not in database");
        }
    }

    // Helper Methods 

    private boolean isUserExists(String login) {
        if (login.contains("@")) {
            return userRepository.existsUserByEmailIgnoreCase(login);
        }
        return userRepository.existsUserByPhoneNumber(login);
    }

    private User createNewUser(RegisterRequest request) {
        User user = new User();
        if (request.login().contains("@")) {
            user.setEmail(request.login());
        } else {
            user.setPhoneNumber(request.login());
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setMiddleName(request.middleName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole(UserRole.USER);
        return user;
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(5, ChronoUnit.DAYS));
        return refreshToken;
    }

    private void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token expired. Please sign in again");
        }
    }

}
