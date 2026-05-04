package com.codingas.gateway.infrastructure.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Aes256EncryptionService 完整单元测试
 */
@DisplayName("Aes256EncryptionService 测试")
class Aes256EncryptionServiceTest {

    private Aes256EncryptionService encryptionService;
    private Environment environment;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        encryptionService = new Aes256EncryptionService(environment);
    }

    @Nested
    @DisplayName("初始化测试")
    class InitTests {

        @Test
        @DisplayName("使用有效密钥初始化成功")
        void init_validKey_initializesSuccessfully() {
            // given
            String validKey = generateValidKey();
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", validKey);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

            // when
            encryptionService.init();

            // then - 无异常抛出
            assertThat(ReflectionTestUtils.getField(encryptionService, "secretKey")).isNotNull();
        }

        @Test
        @DisplayName("开发环境无密钥时生成临时密钥")
        void init_devEnvironmentNoKey_generatesTemporaryKey() {
            // given
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", null);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            // when
            encryptionService.init();

            // then - 无异常抛出
            assertThat(ReflectionTestUtils.getField(encryptionService, "secretKey")).isNotNull();
        }

        @Test
        @DisplayName("生产环境无密钥时抛出异常")
        void init_prodEnvironmentNoKey_throwsException() {
            // given
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", null);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

            // when & then
            assertThatThrownBy(() -> encryptionService.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Encryption key must be configured");
        }

        @Test
        @DisplayName("密钥长度不正确时抛出异常")
        void init_invalidKeyLength_throwsException() {
            // given
            String invalidKey = Base64.getEncoder().encodeToString("short".getBytes());
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", invalidKey);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

            // when & then
            assertThatThrownBy(() -> encryptionService.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
        }
    }

    @Nested
    @DisplayName("加密解密测试")
    class EncryptDecryptTests {

        @BeforeEach
        void setUpService() {
            String validKey = generateValidKey();
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", validKey);
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            encryptionService.init();
        }

        @Test
        @DisplayName("加密后的内容可以解密")
        void encrypt_decrypt_returnsOriginal() {
            // given
            String original = "Hello, World! 这是一段中文测试。";

            // when
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("每次加密产生不同的密文")
        void encrypt_sameText_producesDifferentCiphertext() {
            // given
            String text = "same text";

            // when
            String encrypted1 = encryptionService.encrypt(text);
            String encrypted2 = encryptionService.encrypt(text);

            // then - 由于随机 IV，每次加密结果不同
            assertThat(encrypted1).isNotEqualTo(encrypted2);
            assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(text);
            assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(text);
        }

        @Test
        @DisplayName("加密 null 返回 null")
        void encrypt_null_returnsNull() {
            assertThat(encryptionService.encrypt(null)).isNull();
        }

        @Test
        @DisplayName("解密 null 返回 null")
        void decrypt_null_returnsNull() {
            assertThat(encryptionService.decrypt(null)).isNull();
        }

        @Test
        @DisplayName("加密空字符串")
        void encrypt_emptyString_returnsEncrypted() {
            // when
            String encrypted = encryptionService.encrypt("");
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("加密长文本")
        void encrypt_longText_successfullyEncrypts() {
            // given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("This is a long text for testing. ");
            }
            String longText = sb.toString();

            // when
            String encrypted = encryptionService.encrypt(longText);
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(longText);
        }

        @Test
        @DisplayName("解密无效内容抛出异常")
        void decrypt_invalidContent_throwsException() {
            // when & then
            assertThatThrownBy(() -> encryptionService.decrypt("not-a-valid-encrypted-string"))
                .isInstanceOf(RuntimeException.class);
        }
    }

    // Helper methods
    private String generateValidKey() {
        // 生成 32 字节（256 位）的有效密钥
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) i;
        }
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
