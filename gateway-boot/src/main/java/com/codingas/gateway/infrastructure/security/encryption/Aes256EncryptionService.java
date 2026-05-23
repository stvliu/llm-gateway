package com.codingas.gateway.infrastructure.security.encryption;

import com.codingas.gateway.domain.iam.exception.IamException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
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
 * <p>密钥获取优先级: 1. Spring 配置 2. 环境变量 ENCRYPTION_KEY 3. 开发环境临时密钥</p>
 */
@Slf4j
@Component
public class Aes256EncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 96 bits
    private static final int TAG_SIZE = 128; // 128 bits
    private static final String ENV_KEY = "ENCRYPTION_KEY";

    /**
     * 允许使用临时密钥的 Spring profiles
     * 仅用于开发和测试环境，生产环境必须配置密钥
     */
    private static final String[] DEV_PROFILES = {"dev", "test", "local", "development"};

    @Value("${gateway.security.encryption-key:#{null}}")
    private String encryptionKey;

    private final Environment environment;
    private SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public Aes256EncryptionService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // 优先级: 1. Spring 配置
        String keySource = encryptionKey;

        // 处理 Spring 属性占位符未解析的情况
        if (keySource != null && (keySource.startsWith("${") || keySource.isBlank())) {
            keySource = null;
        }

        if (keySource == null) {
            // 检查是否为开发环境
            if (!isDevelopmentEnvironment()) {
                // 生产环境必须配置加密密钥
                throw new IamException(
                    "ENCRYPTION_KEY_MISSING",
                    "Encryption key must be configured in production environment. " +
                    "Set 'gateway.security.encryption-key' property or 'ENCRYPTION_KEY' environment variable."
                );
            }
            // 开发环境允许使用临时密钥
            log.warn("No encryption key configured, generating temporary key for development only. " +
                     "DO NOT use this in production!");
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE, secureRandom);
                this.secretKey = new SecretKeySpec(keyGen.generateKey().getEncoded(), "AES");
                log.warn("Using temporary encryption key - encrypted data will be lost after restart!");
            } catch (Exception e) {
                throw new IamException("ENCRYPTION_KEY_GENERATION_FAILED", "Failed to generate encryption key", e);
            }
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(keySource);
                if (keyBytes.length != KEY_SIZE / 8) {
                    throw new IamException("ENCRYPTION_KEY_INVALID", "Encryption key must be 256 bits (32 bytes) when decoded");
                }
                this.secretKey = new SecretKeySpec(keyBytes, "AES");
                log.info("Aes256EncryptionService initialized with AES-256-GCM");
            } catch (IllegalArgumentException e) {
                // Base64 解码失败，检查是否为开发环境
                if (!isDevelopmentEnvironment()) {
                    throw new IamException("ENCRYPTION_KEY_INVALID_FORMAT", "Invalid encryption key format: " + e.getMessage(), e);
                }
                log.warn("Invalid encryption key format, generating temporary key for development: {}", e.getMessage());
                try {
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(KEY_SIZE, secureRandom);
                    this.secretKey = new SecretKeySpec(keyGen.generateKey().getEncoded(), "AES");
                    log.warn("Using temporary encryption key - encrypted data will be lost after restart!");
                } catch (Exception ex) {
                    throw new IamException("ENCRYPTION_KEY_GENERATION_FAILED", "Failed to generate encryption key", ex);
                }
            }
        }
    }

    /**
     * 判断是否为开发环境
     *
     * @return true 如果是开发或测试环境
     */
    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
            .anyMatch(profile -> Arrays.asList(DEV_PROFILES).contains(profile.toLowerCase()));
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
            throw new IamException("ENCRYPTION_FAILED", "Encryption failed", e);
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
            throw new IamException("DECRYPTION_FAILED", "Decryption failed", e);
        }
    }
}
