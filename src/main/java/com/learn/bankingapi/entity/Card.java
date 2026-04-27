package com.learn.bankingapi.entity;

import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.CardType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true, length = 19)
    private String cardNumber;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "pin_hash")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType type;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = CardStatus.ACTIVE;
        }

        if (this.expirationDate == null) {
            this.expirationDate = LocalDate.now()
                    .plusYears(4)
                    .with(TemporalAdjusters.lastDayOfMonth());
        }
    }

    public Card() {}


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}