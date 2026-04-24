package com.codingas.gateway.core.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * ApiKeyEncryptionService 单元测试
 *
 * <p>测试加密/解密、哈希和密钥格式验证功能</p>
 */
@DisplayName("ApiKeyEncryptionService")
class ApiKeyEncryptionServiceTest {

    private ApiKeyEncryptionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ApiKeyEncryptionService();
        // 通过反射调用 @PostConstruct 方法，注入测试用密钥
        Field secretKeyField = ApiKeyEncryptionService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);

        // 设置测试用固定密钥（32字节 Base64 编码）
        String testKeyBase64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
        javax.crypto.spec.SecretKeySpec testKey =
            new javax.crypto.spec.SecretKeySpec(
                java.util.Base64.getDecoder().decode(testKeyBase64), "AES");
        secretKeyField.set(service, testKey);
    }

    @Nested
    @DisplayName("encrypt() 与 decrypt()")
    class EncryptDecryptTests {

        @Test
        @DisplayName("加密后解密应返回原始明文")
        void encryptDecrypt_roundTrip_returnsOriginal() {
            String plainText = "sk-test-api-key-12345";

            String encrypted = service.encrypt(plainText);
            String decrypted = service.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("加密不同内容应解密为正确明文")
        void encryptDecrypt_variousInputs_correctDecryption() {
            String[] testCases = {
                "sk-short",
                "sk-ant-api01-test-key-longer-than-100-chars-for-stress-testing-encryption",
                "中文密钥测试",
                "!@#$%^&*()_+-=[]{}|;':\",./<>?",
                ""
            };

            for (String plainText : testCases) {
                if (plainText.isEmpty()) continue; // 空字符串会被拒绝

                String encrypted = service.encrypt(plainText);
                String decrypted = service.decrypt(encrypted);

                assertThat(decrypted)
                    .as("解密 '%s' 应等于原始输入", plainText)
                    .isEqualTo(plainText);
            }
        }

        @Test
        @DisplayName("相同输入加密应产生不同密文（不同 IV）")
        void encrypt_sameInput_differentOutputs() {
            String plainText = "sk-identical-key-00000";

            String encrypted1 = service.encrypt(plainText);
            String encrypted2 = service.encrypt(plainText);

            // 加密结果应不同（因为使用了不同的 IV）
            assertThat(encrypted1)
                .as("两次加密同一输入应产生不同结果")
                .isNotEqualTo(encrypted2);

            // 但两次加密都应能正确解密为原始明文
            assertThat(service.decrypt(encrypted1)).isEqualTo(plainText);
            assertThat(service.decrypt(encrypted2)).isEqualTo(plainText);
        }

        @Test
        @DisplayName("加密结果格式应为 iv:ciphertext")
        void encrypt_outputFormat_correct() {
            String encrypted = service.encrypt("sk-test-key");

            String[] parts = encrypted.split(":");
            assertThat(parts).hasSize(2);

            // IV 部分应为 12 字节的 Base64 编码（16 字符）
            assertThat(parts[0]).hasSize(16);
            // ciphertext 部分应为非空
            assertThat(parts[1]).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("decrypt() 异常处理")
    class DecryptExceptionTests {

        @Test
        @DisplayName("解密 null 应抛出异常")
        void decrypt_null_throwsException() {
            assertThatThrownBy(() -> service.decrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("解密空白字符串应抛出异常")
        void decrypt_blank_throwsException() {
            assertThatThrownBy(() -> service.decrypt("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("解密无效格式（无冒号分隔）应抛出异常")
        void decrypt_invalidFormat_noColon_throwsException() {
            String invalidFormat = "dGVzdGxpYmVyYW5kb20xMjM"; // 无冒号

            assertThatThrownBy(() -> service.decrypt(invalidFormat))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("解密无效 Base64 应抛出异常")
        void decrypt_invalidBase64_throwsException() {
            String invalidBase64 = "not-valid-base64!:base64value";

            assertThatThrownBy(() -> service.decrypt(invalidBase64))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("解密伪造的密文应抛出异常")
        void decrypt_tamperedCiphertext_throwsException() {
            String encrypted = service.encrypt("sk-real-key");
            // 篡改密文部分
            String tampered = encrypted.replace(encrypted.split(":")[1], "AAAAAAAAAAAAAAAAAAAAAA==");

            assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("解密空格式字符串应抛出异常")
        void decrypt_emptyFormat_throwsException() {
            assertThatThrownBy(() -> service.decrypt(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("解密多余冒号格式应抛出异常")
        void decrypt_extraColon_throwsException() {
            // 三个部分会导致解析失败
            String extraColon = "aaa:bbb:ccc";

            assertThatThrownBy(() -> service.decrypt(extraColon))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
        }
    }

    @Nested
    @DisplayName("hashKey()")
    class HashKeyTests {

        @Test
        @DisplayName("相同输入应产生相同哈希")
        void hashKey_sameInput_sameHash() {
            String apiKey = "sk-consistent-key-12345";

            String hash1 = service.hashKey(apiKey);
            String hash2 = service.hashKey(apiKey);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("不同输入应产生不同哈希")
        void hashKey_differentInput_differentHash() {
            String key1 = "sk-key-one";
            String key2 = "sk-key-two";

            String hash1 = service.hashKey(key1);
            String hash2 = service.hashKey(key2);

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("哈希值应为 64 字符的十六进制字符串（SHA-256）")
        void hashKey_outputFormat_correct() {
            String hash = service.hashKey("sk-any-key");

            assertThat(hash)
                .as("SHA-256 输出应为 64 字符 hex 字符串")
                .hasSize(64)
                .matches("^[a-f0-9]+$");
        }

        @Test
        @DisplayName("哈希值不应等于原始密钥")
        void hashKey_notReversible() {
            String apiKey = "sk-secret-api-key-12345";

            String hash = service.hashKey(apiKey);

            assertThat(hash).isNotEqualTo(apiKey);
            assertThat(apiKey).isNotEqualTo(hash); // 确保密钥未被泄露
        }

        @Test
        @DisplayName("哈希 null 应抛出异常")
        void hashKey_null_throwsException() {
            assertThatThrownBy(() -> service.hashKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("哈希空白字符串应抛出异常")
        void hashKey_blank_throwsException() {
            assertThatThrownBy(() -> service.hashKey("   "))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isValidKeyFormat()")
    class IsValidKeyFormatTests {

        @Test
        @DisplayName("sk- 前缀的密钥应验证通过")
        void isValidKeyFormat_skPrefix_valid() {
            assertThat(service.isValidKeyFormat("sk-test123")).isTrue(); // sk-test123 = 10 chars
            assertThat(service.isValidKeyFormat("sk-1234567890")).isTrue();
            assertThat(service.isValidKeyFormat("sk-ant-api01-xxx-yyy-zzz")).isTrue();
        }

        @Test
        @DisplayName("sk-ant- 前缀的 Anthropic 密钥应验证通过")
        void isValidKeyFormat_skAntPrefix_valid() {
            assertThat(service.isValidKeyFormat("sk-ant-api01-xxx")).isTrue();
            assertThat(service.isValidKeyFormat("sk-ant-api01")).isTrue(); // sk-ant-api01 = 13 chars >= 10
        }

        @Test
        @DisplayName("非 sk- 前缀的密钥应验证失败")
        void isValidKeyFormat_nonSkPrefix_invalid() {
            assertThat(service.isValidKeyFormat("pk-xxx")).isFalse();
            assertThat(service.isValidKeyFormat("api_key_xxx")).isFalse();
            assertThat(service.isValidKeyFormat("mykey")).isFalse();
            assertThat(service.isValidKeyFormat("sk")).isFalse();
        }

        @Test
        @DisplayName("短于 10 字符的 sk- 密钥应验证失败")
        void isValidKeyFormat_tooShort_invalid() {
            // sk- 后面少于 7 个字符，总长度 < 10
            assertThat(service.isValidKeyFormat("sk-abc")).isFalse();
            assertThat(service.isValidKeyFormat("sk-ab")).isFalse();
            assertThat(service.isValidKeyFormat("sk-")).isFalse();
            assertThat(service.isValidKeyFormat("sk-a")).isFalse();
        }

        @Test
        @DisplayName("恰好 10 字符的 sk- 密钥应验证通过")
        void isValidKeyFormat_exactly10Chars_valid() {
            assertThat(service.isValidKeyFormat("sk-1234567")).isTrue(); // 10 字符
        }

        @Test
        @DisplayName("null 应返回 false")
        void isValidKeyFormat_null_returnsFalse() {
            assertThat(service.isValidKeyFormat(null)).isFalse();
        }

        @Test
        @DisplayName("空白字符串应返回 false")
        void isValidKeyFormat_blank_returnsFalse() {
            assertThat(service.isValidKeyFormat("")).isFalse();
            assertThat(service.isValidKeyFormat("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("encrypt() 异常处理")
    class EncryptExceptionTests {

        @Test
        @DisplayName("加密 null 应抛出异常")
        void encrypt_null_throwsException() {
            assertThatThrownBy(() -> service.encrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("加密空白字符串应抛出异常")
        void encrypt_blank_throwsException() {
            assertThatThrownBy(() -> service.encrypt("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        }
    }
}