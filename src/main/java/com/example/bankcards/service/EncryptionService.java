package com.example.bankcards.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${app.encryption-key:defaultEncryptionKey123}")
    private String encryptionKey;

    private SecretKeySpec getSecretKey() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] finalKey = new byte[16];
        System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 16));
        return new SecretKeySpec(finalKey, ALGORITHM);
    }

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании данных", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при дешифровании данных", e);
        }
    }

    // Упрощенный метод проверки - проверяем только Base64 формат
    public boolean isEncrypted(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        try {
            // Проверяем, что строка является валидным Base64
            Base64.getDecoder().decode(data);
            // Дополнительно проверяем, что после расшифровки получается что-то осмысленное
            //проверяем, что расшифрованные данные содержат только цифры
            String decrypted = decrypt(data);
            return decrypted.matches("\\d+");
        } catch (Exception e) {
            return false;
        }
    }
}