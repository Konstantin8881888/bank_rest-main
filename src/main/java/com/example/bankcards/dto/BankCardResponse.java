package com.example.bankcards.dto;

import com.example.bankcards.entity.BankCardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankCardResponse {
    private Long id;
    private String maskedCardNumber; // Маскированный номер: **** **** **** 1234
    private String cardHolder;
    private String expiryDate;
    private BankCardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    // Конструкторы
    public BankCardResponse() {}

    public BankCardResponse(Long id, String maskedCardNumber, String cardHolder,
                            String expiryDate, BankCardStatus status, BigDecimal balance,
                            LocalDateTime createdAt) {
        this.id = id;
        this.maskedCardNumber = maskedCardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public BankCardStatus getStatus() { return status; }
    public void setStatus(BankCardStatus status) { this.status = status; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}