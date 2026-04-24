package com.codingas.gateway.core.security.encryption;

import com.codingas.gateway.core.infrastructure.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyEncryptionService 单元测试
 *
 * <p>测试加密/解密、哈希和密钥格式验证功能</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyEncryptionService")
class ApiKeyEncryptionServiceTest {

    @Mock
    private EncryptionService encryptionService;

    private ApiKeyEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyEncryptionService(encryptionService);
    }

    @Nested
    @DisplayName("encrypt() 与 decrypt()")
    class EncryptDecryptTests {

        @Test
        @DisplayName("加密后解密应返回原始明文")
        void encryptDecrypt_roundTrip_returnsOriginal() {
            String plainText = "sk-test-api-key-12345";
            String encrypted = "encrypted_data";
            when(encryptionService.encrypt(plainText)).thenReturn(encrypted);
            when(encryptionService.decrypt(encrypted)).thenReturn(plainText);

            String result = service.encrypt(plainText);
            String decrypted = service.decrypt(encrypted);

            assertThat(result).isEqualTo(encrypted);
            assertThat(decrypted).isEqualTo(plainText);
            verify(encryptionService).encrypt(plainText);
            verify(encryptionService).decrypt(encrypted);
        }

        @Test
        @DisplayName("加密应委托给 EncryptionService")
        void encrypt_delegatesToEncryptionService() {
            String plainText = "sk-test-key";
            when(encryptionService.encrypt(plainText)).thenReturn("mock_encrypted");

            service.encrypt(plainText);

            verify(encryptionService).encrypt(plainText);
        }

        @Test
        @DisplayName("解密应委托给 EncryptionService")
        void decrypt_delegatesToEncryptionService() {
            String encrypted = "mock_encrypted";
            when(encryptionService.decrypt(encrypted)).thenReturn("sk-test-key");

            service.decrypt(encrypted);

            verify(encryptionService).decrypt(encrypted);
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
            assertThat(service.isValidKeyFormat("sk-test123")).isTrue();
            assertThat(service.isValidKeyFormat("sk-1234567890")).isTrue();
            assertThat(service.isValidKeyFormat("sk-ant-api01-xxx-yyy-zzz")).isTrue();
        }

        @Test
        @DisplayName("sk-ant- 前缀的 Anthropic 密钥应验证通过")
        void isValidKeyFormat_skAntPrefix_valid() {
            assertThat(service.isValidKeyFormat("sk-ant-api01-xxx")).isTrue();
            assertThat(service.isValidKeyFormat("sk-ant-api01")).isTrue();
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
            assertThat(service.isValidKeyFormat("sk-abc")).isFalse();
            assertThat(service.isValidKeyFormat("sk-ab")).isFalse();
            assertThat(service.isValidKeyFormat("sk-")).isFalse();
            assertThat(service.isValidKeyFormat("sk-a")).isFalse();
        }

        @Test
        @DisplayName("恰好 10 字符的 sk- 密钥应验证通过")
        void isValidKeyFormat_exactly10Chars_valid() {
            assertThat(service.isValidKeyFormat("sk-1234567")).isTrue();
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