package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BankCardCreateRequest {

    @NotBlank(message = "Номер карты обязателен")
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String cardNumber;

    @NotBlank(message = "Имя владельца обязательно")
    @Size(min = 2, max = 100, message = "Имя владельца должно быть от 2 до 100 символов")
    private String cardHolder;

    @NotBlank(message = "Срок действия обязателен")
    @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}", message = "Срок действия должен быть в формате MM/YY")
    private String expiryDate;

    private Long userId; // Для администратора - создание карты для конкретного пользователя

    // Конструкторы
    public BankCardCreateRequest() {}

    public BankCardCreateRequest(String cardNumber, String cardHolder, String expiryDate, Long userId) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.userId = userId;
    }

    // Геттеры и сеттеры
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}