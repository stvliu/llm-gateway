package com.codingas.gateway.core.infrastructure.encryption;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密服务实现
 *
 * <p>使用 AES-256-GCM 模式提供加密和解密功能:</p>
 * <ul>
 *   <li>256 位密钥</li>
 *   <li>GCM 认证模式 (提供加密和完整性验证)</li>
 *   <li>96 位 IV (初始向量)</li>
 * </ul>
 *
 * <p>加密格式: Base64(IV || 密文 || Auth Tag)</p>
 */
@Component
public class Aes256EncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 96 bits
    private static final int TAG_SIZE = 128; // 128 bits

    @Value("${gateway.security.encryption-key:#{null}}")
    private String encryptionKey;

    private SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException(
                "gateway.security.encryption-key must be configured for AES-256 encryption");
        }
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        if (keyBytes.length != KEY_SIZE / 8) {
            throw new IllegalStateException(
                "Encryption key must be 256 bits (32 bytes) when decoded");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            // 生成随机 IV
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            // 组合: IV || cipherText (包含 auth tag)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            // 解析: IV ||密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            // 解密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
