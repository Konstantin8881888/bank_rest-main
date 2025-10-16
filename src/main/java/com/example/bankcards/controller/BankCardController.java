package com.example.bankcards.controller;

import com.example.bankcards.dto.BankCardCreateRequest;
import com.example.bankcards.dto.BankCardResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.service.BankCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Bank Cards", description = "API для управления банковскими картами")
@SecurityRequirement(name = "bearerAuth")
public class BankCardController {

    @Autowired
    private BankCardService bankCardService;

    // Вспомогательный метод для преобразования BankCard в BankCardResponse
    private BankCardResponse convertToResponse(BankCard card) {
        String maskedNumber = bankCardService.maskCardNumber(card.getCardNumber());

        return new BankCardResponse(
                card.getId(),
                maskedNumber,
                card.getCardHolder(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getCreatedAt()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создание новой карты", description = "Только для администраторов")
    public ResponseEntity<?> createCard(@Valid @RequestBody BankCardCreateRequest request) {
        try {
            BankCard card = bankCardService.createCard(
                    request.getCardNumber(),
                    request.getCardHolder(),
                    request.getExpiryDate(),
                    request.getUserId()
            );

            BankCardResponse response = convertToResponse(card);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получение всех карт", description = "Только для администраторов")
    public ResponseEntity<Page<BankCardResponse>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BankCard> cardsPage = bankCardService.getAllCards(pageable);

        Page<BankCardResponse> responsePage = cardsPage.map(this::convertToResponse);
        return ResponseEntity.ok(responsePage);
    }

    @PutMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Блокировка всех карт", description = "Только для администраторов")
    public ResponseEntity<?> blockCard(@PathVariable Long cardId) {
        try {
            BankCard card = bankCardService.blockCard(cardId);
            BankCardResponse response = convertToResponse(card);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Активация карты", description = "Только для администраторов")
    public ResponseEntity<?> activateCard(@PathVariable Long cardId) {
        try {
            BankCard card = bankCardService.activateCard(cardId);
            BankCardResponse response = convertToResponse(card);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удаление карты", description = "Только для администраторов")
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId) {
        try {
            bankCardService.deleteCard(cardId);
            return ResponseEntity.ok("Карта успешно удалена");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получение всех своих карт", description = "для всех")
    public ResponseEntity<Page<BankCardResponse>> getMyCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BankCard> cardsPage = bankCardService.getUserCards(pageable);

        Page<BankCardResponse> responsePage = cardsPage.map(this::convertToResponse);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/my/{cardId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получение конкретной своей карты", description = "для всех")
    public ResponseEntity<?> getMyCard(@PathVariable Long cardId) {
        try {
            BankCard card = bankCardService.getUserCardById(cardId);
            BankCardResponse response = convertToResponse(card);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/my/{cardId}/block")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Запрос на блокировку своей карты", description = "для всех")
    public ResponseEntity<?> requestBlockMyCard(@PathVariable Long cardId) {
        try {
            BankCard card = bankCardService.requestBlockCard(cardId);
            BankCardResponse response = convertToResponse(card);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Перевод между своими картами", description = "для всех")
    public ResponseEntity<?> transferBetweenCards(@Valid @RequestBody TransferRequest request) {
        try {
            bankCardService.transferBetweenUserCards(
                    request.getFromCardId(),
                    request.getToCardId(),
                    request.getAmount()
            );
            return ResponseEntity.ok("Перевод успешно выполнен");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}