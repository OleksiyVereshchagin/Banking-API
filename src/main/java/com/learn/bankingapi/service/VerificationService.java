package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.auth.ResendRequest;
import com.learn.bankingapi.dto.request.auth.VerifyRequest;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.entity.VerificationToken;
import com.learn.bankingapi.enums.VerificationType;
import com.learn.bankingapi.repository.UserRepository;
import com.learn.bankingapi.repository.VerificationTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static com.learn.bankingapi.enums.VerificationType.PHONE;
@Service
@Transactional
public class VerificationService {
    private final JavaMailSender mailSender;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public VerificationService(JavaMailSender mailSender, VerificationTokenRepository verificationTokenRepository, UserRepository userRepository, JwtService jwtService) {
        this.mailSender = mailSender;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse verify(VerifyRequest request) {
        return switch (request.type()) {
            case EMAIL -> {
                User user = findUserByLogin(request.login());
                VerificationToken verificationToken = findTokenByUser(user);

                if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new ResponseStatusException(HttpStatus.GONE, "Verification code has expired");
                }
                if (!verificationToken.getVerificationCode().equals(request.token())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
                }

                user.setEmailVerified(true);
                userRepository.save(user);
                verificationTokenRepository.delete(verificationToken);

                yield new AuthResponse(jwtService.generateToken(user));
            }
            // TODO: Phone verification
            case PHONE -> throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Phone verification not implemented yet");
        };
    }

    public void sendVerificationCode(User user, String login, VerificationType type) {
        String code = generateToken();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setVerificationCode(code);
        verificationToken.setType(type);
        verificationToken.setCreatedAt(LocalDateTime.now());
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        verificationToken.setResendCounter(0);
        verificationToken.setTimeOfLastResend(null);

        verificationTokenRepository.save(verificationToken);
        sendMailSafely(code, login);
    }

    public void resend(ResendRequest request) {
        if (request.login().contains("@")) {
            User user = findUserByLogin(request.login());
            VerificationToken token = findTokenByUser(user);

            if (token.getResendCounter() >= 3) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Resend limit exceeded");
            }
            if (token.getTimeOfLastResend() != null &&
                    LocalDateTime.now().isBefore(token.getTimeOfLastResend().plusMinutes(1))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before resending verification");
            }

            String code = generateToken();
            token.setVerificationCode(code);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            token.setResendCounter(token.getResendCounter() + 1);
            token.setTimeOfLastResend(LocalDateTime.now());
            verificationTokenRepository.save(token);

            sendMailSafely(code, request.login());
        }
        // TODO: Phone verification
    }

    private User findUserByLogin(String login) {
        if (login.contains("@")) {
            return userRepository.findUserByEmailIgnoreCase(login)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found user by credentials"));
        }
        // TODO: Phone lookup will be added after SMS provider integration
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Phone verification not implemented yet");
    }

    private VerificationToken findTokenByUser(User user) {
        return verificationTokenRepository.findByUser_id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found verification token"));
    }

    private void sendMailSafely(String code, String login) {
        try {
            sendMail(code, login);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to send verification code");
        }
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            codeBuilder.append(random.nextInt(10));
        }
        return codeBuilder.toString();
    }

    private void sendMail(String code, String login) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("banking.api.pet@gmail.com");
        mailMessage.setTo(login);
        mailMessage.setSubject("Verification Code");
        mailMessage.setText("Your code: " + code);
        mailSender.send(mailMessage);
    }
}