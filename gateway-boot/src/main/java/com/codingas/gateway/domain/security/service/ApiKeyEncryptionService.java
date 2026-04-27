package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.common.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * API Key 加密服务
 *
 * <p>封装 API Key 的加密/解密和哈希操作。</p>
 * <p>实际加密委托给 {@link EncryptionService} (AES-256-GCM 实现)。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyEncryptionService {

    private final EncryptionService encryptionService;

    /**
     * 加密 API Key
     *
     * @param plainText 明文 API Key
     * @return 密文
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Plain text cannot be null or empty");
        }
        return encryptionService.encrypt(plainText);
    }

    /**
     * 解密 API Key
     *
     * @param encryptedText 密文
     * @return 明文 API Key
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }
        return encryptionService.decrypt(encryptedText);
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
