package com.learn.bankingapi.entity;

import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "idempotency_key"})
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💰 сума
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // 💸 комісія (опціонально)
    @Column(precision = 19, scale = 4)
    private BigDecimal commission;

    // 🔄 тип операції
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // 📊 статус
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    // 🔐 idempotency key (БЕЗ unique=true)
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    // 👤 user (для idempotency)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 💱 валюта
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    // 📝 повідомлення (для FAILED і т.д.)
    private String message;

    // ⏱ час створення
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ⬅️ звідки
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    // ➡️ куди
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Transaction() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }
}