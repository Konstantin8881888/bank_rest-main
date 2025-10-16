package com.example.bankcards.service;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.BankCardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankCardService {

    @Autowired
    private BankCardRepository bankCardRepository;

    @Autowired
    private UserRepository userRepository;

    // Вспомогательный метод для маскировки номера карты
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    // Получить текущего аутентифицированного пользователя
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    // Создание новой карты (для администратора)
    @Transactional
    public BankCard createCard(String cardNumber, String cardHolder, String expiryDate, Long userId) {
        // Проверяем, существует ли уже карта с таким номером
        if (bankCardRepository.existsByCardNumber(cardNumber)) {
            throw new BadRequestException("Карта с таким номером уже существует");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        BankCard card = new BankCard();
        card.setCardNumber(cardNumber); // Пока без шифрования, добавим позже
        card.setCardHolder(cardHolder);
        card.setExpiryDate(expiryDate);
        card.setStatus(BankCardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setUser(user);

        return bankCardRepository.save(card);
    }

    // Получить все карты текущего пользователя с пагинацией
    public Page<BankCard> getUserCards(Pageable pageable) {
        User currentUser = getCurrentUser();
        return bankCardRepository.findByUser(currentUser, pageable);
    }

    // Получить все карты (для администратора)
    public Page<BankCard> getAllCards(Pageable pageable) {
        return bankCardRepository.findAll(pageable);
    }

    // Получить конкретную карту текущего пользователя
    public BankCard getUserCardById(Long cardId) {
        User currentUser = getCurrentUser();
        return bankCardRepository.findByIdAndUser(cardId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));
    }

    // Запрос на блокировку карты (для пользователя)
    @Transactional
    public BankCard requestBlockCard(Long cardId) {
        BankCard card = getUserCardById(cardId);

        if (card.getStatus() == BankCardStatus.BLOCKED) {
            throw new BadRequestException("Карта уже заблокирована");
        }

        card.setStatus(BankCardStatus.BLOCKED);
        return bankCardRepository.save(card);
    }

    // Блокировка карты (для администратора)
    @Transactional
    public BankCard blockCard(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        card.setStatus(BankCardStatus.BLOCKED);
        return bankCardRepository.save(card);
    }

    // Активация карты (для администратора)
    @Transactional
    public BankCard activateCard(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        card.setStatus(BankCardStatus.ACTIVE);
        return bankCardRepository.save(card);
    }

    // Удаление карты (для администратора)
    @Transactional
    public void deleteCard(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        bankCardRepository.delete(card);
    }

    // Перевод между картами текущего пользователя
    @Transactional
    public void transferBetweenUserCards(Long fromCardId, Long toCardId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Сумма перевода должна быть положительной");
        }

        User currentUser = getCurrentUser();

        // Получаем карту отправителя
        BankCard fromCard = bankCardRepository.findByIdAndUser(fromCardId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта отправителя не найдена"));

        // Получаем карту получателя
        BankCard toCard = bankCardRepository.findByIdAndUser(toCardId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта получателя не найдена"));

        // Проверяем, что это разные карты
        if (fromCard.getId().equals(toCard.getId())) {
            throw new BadRequestException("Нельзя переводить на ту же карту");
        }

        // Проверяем статус карт
        if (fromCard.getStatus() != BankCardStatus.ACTIVE) {
            throw new BadRequestException("Карта отправителя не активна");
        }

        if (toCard.getStatus() != BankCardStatus.ACTIVE) {
            throw new BadRequestException("Карта получателя не активна");
        }

        // Проверяем достаточность средств
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Недостаточно средств на карте отправителя");
        }

        // Выполняем перевод
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        bankCardRepository.save(fromCard);
        bankCardRepository.save(toCard);
    }
}