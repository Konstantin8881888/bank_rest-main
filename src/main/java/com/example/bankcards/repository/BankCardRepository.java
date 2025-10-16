package com.example.bankcards.repository;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankCardRepository extends JpaRepository<BankCard, Long> {

    // Все карты пользователя
    List<BankCard> findByUser(User user);

    // Все карты пользователя с пагинацией
    Page<BankCard> findByUser(User user, Pageable pageable);

    // Конкретная карта пользователя
    Optional<BankCard> findByIdAndUser(Long id, User user);

    // Проверка существования номера карты
    boolean existsByCardNumber(String cardNumber);
}