package com.codingas.gateway.core.infrastructure.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * StubEncryptionService 单元测试
 */
@DisplayName("StubEncryptionService Tests")
class StubEncryptionServiceTest {

    private StubEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new StubEncryptionService();
    }

    @Nested
    @DisplayName("encrypt")
    class EncryptTests {

        @Test
        @DisplayName("加密返回原始明文 (stub 实现)")
        void encrypt_returnsPlainText() {
            String plainText = "sk-test-api-key-12345";

            String result = encryptionService.encrypt(plainText);

            assertThat(result).isEqualTo(plainText);
        }

        @Test
        @DisplayName("加密空字符串")
        void encrypt_emptyString() {
            String result = encryptionService.encrypt("");

            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("加密特殊字符")
        void encrypt_specialCharacters() {
            String plainText = "key_with_special_chars_!@#$%^&*()";

            String result = encryptionService.encrypt(plainText);

            assertThat(result).isEqualTo(plainText);
        }
    }

    @Nested
    @DisplayName("decrypt")
    class DecryptTests {

        @Test
        @DisplayName("解密返回原始密文 (stub 实现)")
        void decrypt_returnsCipherText() {
            String cipherText = "encrypted_key_12345";

            String result = encryptionService.decrypt(cipherText);

            assertThat(result).isEqualTo(cipherText);
        }

        @Test
        @DisplayName("解密空字符串")
        void decrypt_emptyString() {
            String result = encryptionService.decrypt("");

            assertThat(result).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt 对称性")
    class SymmetryTests {

        @Test
        @DisplayName("加密后解密应返回原始内容")
        void encryptThenDecrypt_returnsOriginal() {
            String original = "my-secret-api-key";

            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("多次加密结果应一致")
        void multipleEncrypt_resultsEqual() {
            String plainText = "test-key";

            String result1 = encryptionService.encrypt(plainText);
            String result2 = encryptionService.encrypt(plainText);

            assertThat(result1).isEqualTo(result2);
        }
    }
}