package com.learn.bankingapi.entity;

import com.learn.bankingapi.enums.VerificationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "verification_token", length = 6)
    private String verificationToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "time_of_last_resend")
    private LocalDateTime timeOfLastResend;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "resend_counter", nullable = false)
    private int resendCounter;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VerificationType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    public VerificationToken() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVerificationCode() {
        return verificationToken;
    }

    public void setVerificationCode(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getTimeOfLastResend() {
        return timeOfLastResend;
    }

    public void setTimeOfLastResend(LocalDateTime timeOfLastResend) {
        this.timeOfLastResend = timeOfLastResend;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getResendCounter() {
        return resendCounter;
    }

    public void setResendCounter(int resendCounter) {
        this.resendCounter = resendCounter;
    }

    public VerificationType getType() {
        return type;
    }

    public void setType(VerificationType type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
