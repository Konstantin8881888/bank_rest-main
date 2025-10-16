package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.SignupRequest;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional // Добавляем транзакционность для автоматического отката
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankCardRepository bankCardRepository; // Добавляем для правильной очистки

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Очищаем в правильном порядке из-за foreign key constraints
        bankCardRepository.deleteAll(); // Сначала карты
        userRepository.deleteAll(); // Потом пользователей

        // Создаем роль USER если её нет
        if (roleRepository.findByName(Role.RoleName.USER).isEmpty()) {
            Role userRole = new Role(Role.RoleName.USER);
            roleRepository.save(userRole);
        }

        // Создаем роль ADMIN если её нет
        if (roleRepository.findByName(Role.RoleName.ADMIN).isEmpty()) {
            Role adminRole = new Role(Role.RoleName.ADMIN);
            roleRepository.save(adminRole);
        }
    }

    @Test
    void registerUser_WithValidData_ShouldReturnSuccess() throws Exception {
        SignupRequest signupRequest = new SignupRequest(
                "testuser",
                "test@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    void registerUser_WithExistingUsername_ShouldReturnError() throws Exception {
        // Сначала создаем пользователя
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));

        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        existingUser.setRoles(roles);

        userRepository.save(existingUser);

        // Пытаемся зарегистрировать с тем же username
        SignupRequest signupRequest = new SignupRequest(
                "existinguser",
                "new@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldReturnError() throws Exception {
        // Сначала создаем пользователя
        User existingUser = new User();
        existingUser.setUsername("user1");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));

        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        existingUser.setRoles(roles);

        userRepository.save(existingUser);

        // Пытаемся зарегистрировать с тем же email
        SignupRequest signupRequest = new SignupRequest(
                "newuser",
                "existing@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // Сначала регистрируем пользователя
        User user = new User();
        user.setUsername("loginuser");
        user.setEmail("login@example.com");
        user.setPassword(passwordEncoder.encode("password123"));

        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("loginuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithWrongPassword_ShouldReturnUnauthorized() throws Exception {
        // Сначала создаем пользователя
        User user = new User();
        user.setUsername("passworduser");
        user.setEmail("password@example.com");
        user.setPassword(passwordEncoder.encode("correctpassword"));

        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("passworduser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}