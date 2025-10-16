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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class BankCardService {

    @Autowired
    private BankCardRepository bankCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Вспомогательный метод для маскировки номера карты
    public String maskCardNumber(String cardNumber) {
        try {
            // Если номер зашифрован, расшифруем его для маскировки
            String decryptedNumber = encryptionService.isEncrypted(cardNumber)
                    ? encryptionService.decrypt(cardNumber)
                    : cardNumber;

            if (decryptedNumber == null || decryptedNumber.length() < 4) {
                return "****";
            }
            String lastFour = decryptedNumber.substring(decryptedNumber.length() - 4);
            return "**** **** **** " + lastFour;
        } catch (Exception e) {
            return "**** **** **** ****";
        }
    }

    // Получить текущего аутентифицированного пользователя
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    // Проверка срока действия карты
    private boolean isCardExpired(String expiryDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth expiryYearMonth = YearMonth.parse(expiryDate, formatter);

            // Получаем последний день месяца срока действия
            LocalDate expiry = expiryYearMonth.atEndOfMonth();

            // Сравниваем с текущей датой (без времени)
            return expiry.isBefore(LocalDate.now());
        } catch (Exception e) {
            // Если не можем распарсить, считаем карту просроченной
            return true;
        }
    }

    // Автоматическая проверка и обновление статуса карты
    private void checkAndUpdateCardStatus(BankCard card) {
        if (isCardExpired(card.getExpiryDate()) && card.getStatus() != BankCardStatus.EXPIRED) {
            card.setStatus(BankCardStatus.EXPIRED);
            bankCardRepository.save(card);
        }
    }

    // Создание новой карты (для администратора)
    @Transactional
    public BankCard createCard(String cardNumber, String cardHolder, String expiryDate, Long userId) {
        // Проверяем валидность номера карты (простая проверка)
        if (!isValidCardNumber(cardNumber)) {
            throw new BadRequestException("Неверный формат номера карты");
        }

        // Шифруем номер карты перед сохранением
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        // Проверяем, существует ли уже карта с таким номером
        if (bankCardRepository.existsByCardNumber(encryptedCardNumber)) {
            throw new BadRequestException("Карта с таким номером уже существует");
        }

        // Проверяем срок действия
        if (isCardExpired(expiryDate)) {
            throw new BadRequestException("Нельзя создать карту с истекшим сроком действия");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        BankCardStatus initialStatus = BankCardStatus.ACTIVE;

        BankCard card = new BankCard();
        card.setCardNumber(encryptedCardNumber);
        card.setCardHolder(cardHolder);
        card.setExpiryDate(expiryDate);
        card.setStatus(initialStatus);
        card.setBalance(BigDecimal.ZERO);
        card.setUser(user);

        return bankCardRepository.save(card);
    }

    // Простая валидация номера карты
    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return false;
        }
        return cardNumber.matches("\\d+");
    }

    // Получить все карты текущего пользователя с пагинацией
    public Page<BankCard> getUserCards(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<BankCard> cards = bankCardRepository.findByUser(currentUser, pageable);

        // Проверяем статусы всех полученных карт
        cards.forEach(this::checkAndUpdateCardStatus);

        return cards;
    }

    // Получить все карты (для администратора)
    public Page<BankCard> getAllCards(Pageable pageable) {
        Page<BankCard> cards = bankCardRepository.findAll(pageable);

        // Проверяем статусы всех полученных карт
        cards.forEach(this::checkAndUpdateCardStatus);

        return cards;
    }

    // Получить конкретную карту текущего пользователя
    public BankCard getUserCardById(Long cardId) {
        User currentUser = getCurrentUser();
        BankCard card = bankCardRepository.findByIdAndUser(cardId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        checkAndUpdateCardStatus(card);
        return card;
    }

    // Запрос на блокировку карты (для пользователя)
    @Transactional
    public BankCard requestBlockCard(Long cardId) {
        BankCard card = getUserCardById(cardId);

        if (card.getStatus() == BankCardStatus.BLOCKED) {
            throw new BadRequestException("Карта уже заблокирована");
        }

        if (card.getStatus() == BankCardStatus.EXPIRED) {
            throw new BadRequestException("Нельзя заблокировать карту с истекшим сроком действия");
        }

        card.setStatus(BankCardStatus.BLOCKED);
        return bankCardRepository.save(card);
    }

    // Блокировка карты (для администратора)
    @Transactional
    public BankCard blockCard(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        checkAndUpdateCardStatus(card);

        if (card.getStatus() == BankCardStatus.BLOCKED) {
            throw new BadRequestException("Карта уже заблокирована");
        }

        card.setStatus(BankCardStatus.BLOCKED);
        return bankCardRepository.save(card);
    }

    // Активация карты (для администратора)
    @Transactional
    public BankCard activateCard(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        checkAndUpdateCardStatus(card);

        if (card.getStatus() == BankCardStatus.ACTIVE) {
            throw new BadRequestException("Карта уже активна");
        }

        if (card.getStatus() == BankCardStatus.EXPIRED) {
            throw new BadRequestException("Нельзя активировать карту с истекшим сроком действия");
        }

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

        // Проверяем статусы карт
        checkAndUpdateCardStatus(fromCard);
        checkAndUpdateCardStatus(toCard);

        // Проверяем, что это разные карты
        if (fromCard.getId().equals(toCard.getId())) {
            throw new BadRequestException("Нельзя переводить на ту же карту");
        }

        // Проверяем статус карт
        if (fromCard.getStatus() != BankCardStatus.ACTIVE) {
            throw new BadRequestException("Карта отправителя не активна. Текущий статус: " + fromCard.getStatus().getDisplayName());
        }

        if (toCard.getStatus() != BankCardStatus.ACTIVE) {
            throw new BadRequestException("Карта получателя не активна. Текущий статус: " + toCard.getStatus().getDisplayName());
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

    // Получить реальный номер карты (только для администратора, с осторожностью)
    public String getDecryptedCardNumber(Long cardId) {
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));

        return encryptionService.decrypt(card.getCardNumber());
    }
}