package com.example.bankcards.service;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.BankCardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankCardServiceTest {

    @Mock
    private BankCardRepository bankCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BankCardService bankCardService;

    private User testUser;
    private BankCard activeCard;
    private BankCard blockedCard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");

        // Используем дату далеко в будущем для тестов
        activeCard = new BankCard();
        activeCard.setId(1L);
        activeCard.setCardNumber("encrypted123");
        activeCard.setCardHolder("Test User");
        activeCard.setExpiryDate("12/50"); // Очень далекая дата
        activeCard.setStatus(BankCardStatus.ACTIVE);
        activeCard.setBalance(new BigDecimal("1000.00"));
        activeCard.setUser(testUser);
        activeCard.setCreatedAt(LocalDateTime.now());

        blockedCard = new BankCard();
        blockedCard.setId(2L);
        blockedCard.setCardNumber("encrypted456");
        blockedCard.setCardHolder("Test User");
        blockedCard.setExpiryDate("12/50"); // Очень далекая дата
        blockedCard.setStatus(BankCardStatus.BLOCKED);
        blockedCard.setBalance(new BigDecimal("500.00"));
        blockedCard.setUser(testUser);
        blockedCard.setCreatedAt(LocalDateTime.now());
    }

    private void mockSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void maskCardNumber_ShouldReturnMaskedNumber() {
        // Arrange
        String cardNumber = "1234567890123456";
        when(encryptionService.isEncrypted(anyString())).thenReturn(false);

        // Act
        String result = bankCardService.maskCardNumber(cardNumber);

        // Assert
        assertEquals("**** **** **** 3456", result);
    }

    @Test
    void createCard_WithValidData_ShouldCreateCard() {
        // Arrange
        String cardNumber = "1234567890123456";
        String cardHolder = "Test User";
        String expiryDate = "12/50"; // Очень далекая дата
        Long userId = 1L;

        when(encryptionService.encrypt(cardNumber)).thenReturn("encrypted123");
        when(bankCardRepository.existsByCardNumber("encrypted123")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bankCardRepository.save(any(BankCard.class))).thenReturn(activeCard);

        // Act
        BankCard result = bankCardService.createCard(cardNumber, cardHolder, expiryDate, userId);

        // Assert
        assertNotNull(result);
        assertEquals(BankCardStatus.ACTIVE, result.getStatus());
        verify(bankCardRepository).save(any(BankCard.class));
    }

    @Test
    void createCard_WithExistingCardNumber_ShouldThrowException() {
        // Arrange
        String cardNumber = "1234567890123456";
        String cardHolder = "Test User";
        String expiryDate = "12/50";
        Long userId = 1L;

        when(encryptionService.encrypt(cardNumber)).thenReturn("encrypted123");
        when(bankCardRepository.existsByCardNumber("encrypted123")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.createCard(cardNumber, cardHolder, expiryDate, userId);
        });
    }

    @Test
    void createCard_WithExpiredDate_ShouldThrowException() {
        // Arrange
        String cardNumber = "1234567890123456";
        String cardHolder = "Test User";
        String expiryDate = "01/20"; // Прошедшая дата
        Long userId = 1L;

        when(encryptionService.encrypt(cardNumber)).thenReturn("encrypted123");
        when(bankCardRepository.existsByCardNumber("encrypted123")).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.createCard(cardNumber, cardHolder, expiryDate, userId);
        });
    }

    @Test
    void getUserCards_ShouldReturnUserCards() {
        // Arrange
        mockSecurityContext();
        Pageable pageable = PageRequest.of(0, 10);
        List<BankCard> cards = Arrays.asList(activeCard, blockedCard);
        Page<BankCard> cardPage = new PageImpl<>(cards, pageable, cards.size());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByUser(testUser, pageable)).thenReturn(cardPage);

        // Act
        Page<BankCard> result = bankCardService.getUserCards(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(bankCardRepository).findByUser(testUser, pageable);
    }

    @Test
    void getUserCardById_WithValidCard_ShouldReturnCard() {
        // Arrange
        mockSecurityContext();
        Long cardId = 1L;

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(cardId, testUser)).thenReturn(Optional.of(activeCard));

        // Act
        BankCard result = bankCardService.getUserCardById(cardId);

        // Assert
        assertNotNull(result);
        assertEquals(cardId, result.getId());
    }

    @Test
    void getUserCardById_WithNonExistentCard_ShouldThrowException() {
        // Arrange
        mockSecurityContext();
        Long cardId = 999L;

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(cardId, testUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            bankCardService.getUserCardById(cardId);
        });
    }

    @Test
    void requestBlockCard_WithActiveCard_ShouldBlockCard() {
        // Arrange
        mockSecurityContext();
        Long cardId = 1L;

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(cardId, testUser)).thenReturn(Optional.of(activeCard));
        when(bankCardRepository.save(any(BankCard.class))).thenReturn(blockedCard);

        // Act
        BankCard result = bankCardService.requestBlockCard(cardId);

        // Assert
        assertNotNull(result);
        assertEquals(BankCardStatus.BLOCKED, result.getStatus());
        verify(bankCardRepository).save(activeCard);
    }

    @Test
    void requestBlockCard_WithAlreadyBlockedCard_ShouldThrowException() {
        // Arrange
        mockSecurityContext();
        Long cardId = 2L;

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(cardId, testUser)).thenReturn(Optional.of(blockedCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.requestBlockCard(cardId);
        });
    }

    @Test
    void transferBetweenUserCards_WithValidData_ShouldTransferSuccessfully() {
        // Arrange
        mockSecurityContext();
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = new BigDecimal("100.00");

        BankCard fromCard = new BankCard();
        fromCard.setId(fromCardId);
        fromCard.setBalance(new BigDecimal("500.00"));
        fromCard.setStatus(BankCardStatus.ACTIVE);
        fromCard.setExpiryDate("12/50"); // Очень далекая дата
        fromCard.setUser(testUser);

        BankCard toCard = new BankCard();
        toCard.setId(toCardId);
        toCard.setBalance(new BigDecimal("200.00"));
        toCard.setStatus(BankCardStatus.ACTIVE);
        toCard.setExpiryDate("12/50"); // Очень далекая дата
        toCard.setUser(testUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(fromCardId, testUser)).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndUser(toCardId, testUser)).thenReturn(Optional.of(toCard));
        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> {
            bankCardService.transferBetweenUserCards(fromCardId, toCardId, amount);
        });

        // Assert
        assertEquals(new BigDecimal("400.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("300.00"), toCard.getBalance());
        verify(bankCardRepository, times(2)).save(any(BankCard.class));
    }

    @Test
    void transferBetweenUserCards_WithInsufficientFunds_ShouldThrowException() {
        // Arrange
        mockSecurityContext();
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = new BigDecimal("600.00");

        BankCard fromCard = new BankCard();
        fromCard.setId(fromCardId);
        fromCard.setBalance(new BigDecimal("500.00"));
        fromCard.setStatus(BankCardStatus.ACTIVE);
        fromCard.setExpiryDate("12/50");
        fromCard.setUser(testUser);

        BankCard toCard = new BankCard();
        toCard.setId(toCardId);
        toCard.setBalance(new BigDecimal("200.00"));
        toCard.setStatus(BankCardStatus.ACTIVE);
        toCard.setExpiryDate("12/50");
        toCard.setUser(testUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(fromCardId, testUser)).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndUser(toCardId, testUser)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.transferBetweenUserCards(fromCardId, toCardId, amount);
        });
    }

    @Test
    void transferBetweenUserCards_WithSameCard_ShouldThrowException() {
        // Arrange
        mockSecurityContext();
        Long cardId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(cardId, testUser)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.transferBetweenUserCards(cardId, cardId, amount);
        });
    }

    @Test
    void transferBetweenUserCards_WithBlockedCard_ShouldThrowException() {
        // Arrange
        mockSecurityContext();
        Long fromCardId = 2L;
        Long toCardId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bankCardRepository.findByIdAndUser(fromCardId, testUser)).thenReturn(Optional.of(blockedCard));
        when(bankCardRepository.findByIdAndUser(toCardId, testUser)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            bankCardService.transferBetweenUserCards(fromCardId, toCardId, amount);
        });
    }
}