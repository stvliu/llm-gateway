package com.codingas.gateway.core.security.encryption;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密服务
 *
 * <p>使用 AES-256-GCM 对 GatewayApiKey 和 ProviderApiKey 进行加密存储。</p>
 * <p>密钥通过环境变量 API_KEY_ENCRYPTION_KEY 注入，不硬编码。</p>
 */
@Slf4j
@Service
public class ApiKeyEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENV_KEY = "API_KEY_ENCRYPTION_KEY";

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        String keyBase64 = System.getenv(ENV_KEY);
        if (keyBase64 == null || keyBase64.isBlank()) {
            log.warn("API_KEY_ENCRYPTION_KEY not set, generating temporary key for development");
            // Generate a temporary key for development only
            KeyGenerator keyGen;
            try {
                keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                secretKey = keyGen.generateKey();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate encryption key", e);
            }
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalStateException("API_KEY_ENCRYPTION_KEY must be 32 bytes (256 bits)");
            }
            secretKey = new SecretKeySpec(keyBytes, "AES");
        }
        log.info("ApiKeyEncryptionService initialized with AES-256-GCM");
    }

    /**
     * 加密 API Key
     *
     * @param plainText 明文 API Key
     * @return 密文 (格式: base64(iv):base64(ciphertext))
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Plain text cannot be null or empty");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String cipherBase64 = Base64.getEncoder().encodeToString(cipherText);
            return ivBase64 + ":" + cipherBase64;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * 解密 API Key
     *
     * @param encryptedText 密文 (格式: base64(iv):base64(ciphertext))
     * @return 明文 API Key
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }
        try {
            String[] parts = encryptedText.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted text format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /**
     * 验证密钥格式是否正确
     *
     * @param apiKey API Key 明文
     * @return true 如果格式正确
     */
    public boolean isValidKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        // GatewayApiKey: sk- 前缀 + 32位加密随机字符串 = 44字符
        // ProviderApiKey: sk- 或 sk-ant- 前缀
        if (apiKey.startsWith("sk-")) {
            return apiKey.length() >= 10;
        }
        return false;
    }

    /**
     * 生成 API Key 哈希 (用于数据库索引查询)
     * 使用 SHA-256 哈希，不加盐（因为 key 本身已经足够随机）
     *
     * @param apiKey API Key 明文
     * @return 哈希值 (hex encoded)
     */
    public String hashKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key cannot be null or empty");
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
