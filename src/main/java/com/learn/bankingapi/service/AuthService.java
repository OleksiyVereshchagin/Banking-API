package com.learn.bankingapi.service;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.entity.RefreshToken;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.dto.request.auth.LoginRequest;
import com.learn.bankingapi.dto.request.auth.RegisterRequest;
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

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthResponse registration(RegisterRequest request){
        String login = request.login();

        boolean exists;

        if (login.contains("@")){
            exists = userRepository.existsUserByEmailIgnoreCase(login);
        } else {
            exists = userRepository.existsUserByPhoneNumber(login);
        }

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with login" + login + " already exists");

        }

        User user = new User();

        if (request.login().contains("@")) {
            user.setEmail(request.login());
        } else {
            user.setPhoneNumber(request.login());
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        user.setPassword(encodedPassword);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setMiddleName(request.middleName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRole(UserRole.USER);

        userRepository.save(user);
        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    public AuthLoginResponse login(LoginRequest request) {
        // 1. Автентифікація користувача через Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.password()
                )
        );

        // 2. Отримання об'єкта користувача з Principal
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // 3. Робота з Refresh Token
        // Перед створенням нового, видаляємо старий токен цього користувача (одна сесія)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString()); // Генеруємо унікальний ідентифікатор
        refreshToken.setExpiryDate(Instant.now().plus(5, ChronoUnit.DAYS)); // Термін дії 5 днів

        refreshTokenRepository.save(refreshToken);

        // 4. Генерація короткострокового Access Token (JWT)
        String accessToken = jwtService.generateToken(user);

        // 5. Повертаємо обидва токени клієнту
        return new AuthLoginResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refreshToken(String requestToken) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(requestToken);

        if (tokenOptional.isPresent()) {
            RefreshToken token = tokenOptional.get();

            verifyExpiration(token);
            User user = token.getUser();
            String accessToken = jwtService.generateToken(user);

            return new AuthResponse(accessToken);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is not in database!");
        }
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token); // Видаляємо прострочений
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Refresh token was expired. Please make a new signin request"
            );
        }
        return token;
    }

}
