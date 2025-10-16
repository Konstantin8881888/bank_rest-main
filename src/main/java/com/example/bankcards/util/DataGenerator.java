package com.example.bankcards.util;

import com.example.bankcards.entity.BankCardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.BankCardService;
import com.example.bankcards.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class DataGenerator implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Создаем тестового пользователя, если его нет
        if (!userService.existsByUsername("testuser")) {
            User testUser = userService.createUser("testuser", "test@example.com", "password123");
            System.out.println("Создан тестовый пользователь: testuser / password123");
        }

        // Создаем несколько тестовых карт для демонстрации
        createDemoCards();
    }

    private void createDemoCards() {
        try {
            User adminUser = userService.createUser("cardadmin", "admin@cards.com", "admin123");

            // Генерируем несколько тестовых карт
            String[] testCards = {
                    "4111111111111111", "5500000000000004", "340000000000009",
                    "6011000000000004", "30000000000004"
            };

            Random random = new Random();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");

            for (int i = 0; i < Math.min(3, testCards.length); i++) {
                String cardNumber = testCards[i];
                String expiryDate = LocalDate.now().plusYears(2 + random.nextInt(3))
                        .format(formatter);
                String cardHolder = "TEST USER " + (i + 1);

                try {
                    bankCardService.createCard(cardNumber, cardHolder, expiryDate, 1L);
                    System.out.println("Создана тестовая карта: " + cardNumber);
                } catch (Exception e) {
                    System.out.println("Карта уже существует: " + cardNumber);
                }
            }

        } catch (Exception e) {
            System.out.println("Демо карты уже созданы или ошибка: " + e.getMessage());
        }
    }
}