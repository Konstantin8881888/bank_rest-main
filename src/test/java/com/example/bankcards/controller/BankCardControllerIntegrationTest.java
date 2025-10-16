package com.example.bankcards.controller;

import com.example.bankcards.dto.BankCardCreateRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.BankCardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
class BankCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankCardRepository bankCardRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;
    private BankCard userCard1;
    private BankCard userCard2;

    @BeforeEach
    void setUp() {
        // Очищаем в правильном порядке
        bankCardRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем роли если их нет
        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.USER)));
        Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.ADMIN)));

        // Создаем тестового пользователя
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        testUser.setRoles(userRoles);
        testUser = userRepository.save(testUser);

        // Создаем администратора
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);
        adminUser = userRepository.save(adminUser);

        // Генерируем JWT токены
        userToken = jwtUtils.generateTokenFromUsername(testUser.getUsername());
        adminToken = jwtUtils.generateTokenFromUsername(adminUser.getUsername());

        // Создаем тестовые карты для пользователя
        userCard1 = new BankCard();
        userCard1.setCardNumber("encrypted1111111111111111");
        userCard1.setCardHolder("Test User");
        userCard1.setExpiryDate("12/30");
        userCard1.setStatus(BankCardStatus.ACTIVE);
        userCard1.setBalance(new BigDecimal("1000.00"));
        userCard1.setUser(testUser);
        userCard1 = bankCardRepository.save(userCard1);

        userCard2 = new BankCard();
        userCard2.setCardNumber("encrypted2222222222222222");
        userCard2.setCardHolder("Test User");
        userCard2.setExpiryDate("12/30");
        userCard2.setStatus(BankCardStatus.ACTIVE);
        userCard2.setBalance(new BigDecimal("500.00"));
        userCard2.setUser(testUser);
        userCard2 = bankCardRepository.save(userCard2);
    }

    @Test
    void getMyCards_WithValidToken_ShouldReturnUserCards() throws Exception {
        mockMvc.perform(get("/api/cards/my")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].maskedCardNumber", containsString("****")));
    }

    @Test
    void getMyCard_WithValidCardId_ShouldReturnCard() throws Exception {
        mockMvc.perform(get("/api/cards/my/{cardId}", userCard1.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userCard1.getId()))
                .andExpect(jsonPath("$.maskedCardNumber", containsString("****")));
    }

    @Test
    void getMyCard_WithOtherUserCard_ShouldReturnNotFound() throws Exception {
        // Создаем карту другого пользователя
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(Role.RoleName.USER).get());
        otherUser.setRoles(roles);
        otherUser = userRepository.save(otherUser);

        BankCard otherUserCard = new BankCard();
        otherUserCard.setCardNumber("encrypted3333333333333333");
        otherUserCard.setCardHolder("Other User");
        otherUserCard.setExpiryDate("12/30");
        otherUserCard.setStatus(BankCardStatus.ACTIVE);
        otherUserCard.setBalance(new BigDecimal("300.00"));
        otherUserCard.setUser(otherUser);
        otherUserCard = bankCardRepository.save(otherUserCard);

        mockMvc.perform(get("/api/cards/my/{cardId}", otherUserCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenCards_WithValidData_ShouldSucceed() throws Exception {
        TransferRequest transferRequest = new TransferRequest(
                userCard1.getId(),
                userCard2.getId(),
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Перевод успешно выполнен"));
    }

    @Test
    void transferBetweenCards_WithInsufficientFunds_ShouldReturnError() throws Exception {
        TransferRequest transferRequest = new TransferRequest(
                userCard1.getId(),
                userCard2.getId(),
                new BigDecimal("5000.00") // Слишком большая сумма
        );

        mockMvc.perform(post("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestBlockMyCard_WithActiveCard_ShouldBlockCard() throws Exception {
        mockMvc.perform(put("/api/cards/my/{cardId}/block", userCard1.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    // Тесты для администраторских endpoints
    @Test
    void getAllCards_AsAdmin_ShouldReturnAllCards() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getAllCards_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_AsAdmin_WithValidData_ShouldCreateCard() throws Exception {
        String futureDate = LocalDate.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));

        BankCardCreateRequest createRequest = new BankCardCreateRequest(
                "5555444433332222",
                "New Card Holder",
                futureDate,
                testUser.getId()
        );

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedCardNumber", containsString("****")));
    }

    @Test
    void createCard_AsUser_ShouldReturnForbidden() throws Exception {
        BankCardCreateRequest createRequest = new BankCardCreateRequest(
                "5555444433332222",
                "New Card Holder",
                "12/30",
                testUser.getId()
        );

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyCards_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cards/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    // Добавляем тест для проверки 403
    @Test
    void accessAdminEndpoint_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}