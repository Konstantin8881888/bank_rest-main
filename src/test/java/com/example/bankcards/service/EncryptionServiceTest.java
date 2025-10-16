package com.example.bankcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "testEncryptionKey12345678901234567890123456789012");
    }

    @Test
    void encryptAndDecrypt_ShouldWorkCorrectly() {
        // Arrange
        String originalText = "4111111111111111";

        // Act
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void isEncrypted_WithEncryptedText_ShouldReturnTrue() {
        // Arrange
        String originalText = "5500000000000004";
        String encrypted = encryptionService.encrypt(originalText);

        // Act
        boolean result = encryptionService.isEncrypted(encrypted);

        // Assert
        assertTrue(result);
    }

    @Test
    void isEncrypted_WithPlainText_ShouldReturnFalse() {
        // Arrange
        String plainText = "thisisplaintext";

        // Act
        boolean result = encryptionService.isEncrypted(plainText);

        // Assert
        assertFalse(result);
    }

    @Test
    void encrypt_WithNull_ShouldThrowException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            encryptionService.encrypt(null);
        });
    }

    @Test
    void decrypt_WithInvalidData_ShouldThrowException() {
        // Arrange
        String invalidEncryptedData = "invalidBase64Data";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidEncryptedData);
        });
    }
}