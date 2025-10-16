package com.example.bankcards.repository;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.BankCardStatus;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
class BankCardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankCardRepository bankCardRepository;

    @Test
    void findByUser_ShouldReturnUserCards() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        User savedUser = entityManager.persistAndFlush(user);

        BankCard card1 = new BankCard();
        card1.setCardNumber("1234567890123456");
        card1.setCardHolder("Test User");
        card1.setExpiryDate("12/30");
        card1.setStatus(BankCardStatus.ACTIVE);
        card1.setBalance(new BigDecimal("1000.00"));
        card1.setUser(savedUser);
        entityManager.persistAndFlush(card1);

        BankCard card2 = new BankCard();
        card2.setCardNumber("9876543210987654");
        card2.setCardHolder("Test User");
        card2.setExpiryDate("12/30");
        card2.setStatus(BankCardStatus.ACTIVE);
        card2.setBalance(new BigDecimal("500.00"));
        card2.setUser(savedUser);
        entityManager.persistAndFlush(card2);

        // Act
        List<BankCard> result = bankCardRepository.findByUser(savedUser);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(card -> card.getUser().getId().equals(savedUser.getId())));
    }

    @Test
    void findByUser_WithPageable_ShouldReturnPagedResults() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        User savedUser = entityManager.persistAndFlush(user);

        for (int i = 0; i < 5; i++) {
            BankCard card = new BankCard();
            card.setCardNumber("12345678901234" + i);
            card.setCardHolder("Test User " + i);
            card.setExpiryDate("12/30");
            card.setStatus(BankCardStatus.ACTIVE);
            card.setBalance(new BigDecimal("1000.00"));
            card.setUser(savedUser);
            entityManager.persistAndFlush(card);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // Act
        Page<BankCard> result = bankCardRepository.findByUser(savedUser, pageable);

        // Assert
        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void findByIdAndUser_WithValidData_ShouldReturnCard() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        User savedUser = entityManager.persistAndFlush(user);

        BankCard card = new BankCard();
        card.setCardNumber("1234567890123456");
        card.setCardHolder("Test User");
        card.setExpiryDate("12/30");
        card.setStatus(BankCardStatus.ACTIVE);
        card.setBalance(new BigDecimal("1000.00"));
        card.setUser(savedUser);
        BankCard savedCard = entityManager.persistAndFlush(card);

        // Act
        Optional<BankCard> result = bankCardRepository.findByIdAndUser(savedCard.getId(), savedUser);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(savedCard.getId(), result.get().getId());
        assertEquals(savedUser.getId(), result.get().getUser().getId());
    }

    @Test
    void existsByCardNumber_WhenCardExists_ShouldReturnTrue() {
        // Arrange
        String cardNumber = "1234567890123456";

        BankCard card = new BankCard();
        card.setCardNumber(cardNumber);
        card.setCardHolder("Test User");
        card.setExpiryDate("12/30");
        card.setStatus(BankCardStatus.ACTIVE);
        card.setBalance(new BigDecimal("1000.00"));

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        User savedUser = entityManager.persistAndFlush(user);
        card.setUser(savedUser);

        entityManager.persistAndFlush(card);

        // Act
        boolean result = bankCardRepository.existsByCardNumber(cardNumber);

        // Assert
        assertTrue(result);
    }
}