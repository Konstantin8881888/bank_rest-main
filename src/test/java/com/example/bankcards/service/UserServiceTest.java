package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(2L);
        userRole.setName(Role.RoleName.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void existsByUsername_WhenUserExists_ShouldReturnTrue() {
        // Arrange
        String username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername(username);

        // Assert
        assertTrue(result);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByUsername_WhenUserNotExists_ShouldReturnFalse() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // Act
        boolean result = userService.existsByUsername(username);

        // Assert
        assertFalse(result);
    }

    @Test
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertTrue(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        // Arrange
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";

        User newUser = new User(username, email, encodedPassword);

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(roleRepository.findByName(Role.RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(username, email, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(password);
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        // Arrange
        String username = "existinguser";
        String email = "new@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.createUser(username, email, password);
        });

        assertTrue(exception.getMessage().contains("Username is already taken"));
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        // Arrange
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.createUser(username, email, password);
        });

        assertTrue(exception.getMessage().contains("Email is already in use"));
    }

    @Test
    void createUser_WhenRoleNotFound_ShouldThrowException() {
        // Arrange
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(roleRepository.findByName(Role.RoleName.USER)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(username, email, password);
        });

        assertTrue(exception.getMessage().contains("Role USER not found"));
    }
}