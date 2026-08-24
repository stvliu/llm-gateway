/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.encryption;

import com.codingas.gateway.iam.exception.IamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Aes256EncryptionService 单元测试：加密/解密/错误密钥/边界断言。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Aes256EncryptionService 测试")
class Aes256EncryptionServiceTest {

    @Mock
    private Environment environment;

    private Aes256EncryptionService service;

    @BeforeEach
    void setUp() {
        service = new Aes256EncryptionService(environment);
    }

    /** 通过反射设置 @Value 注入的 encryptionKey 字段 */
    private void setEncryptionKey(String key) throws Exception {
        Field field = Aes256EncryptionService.class.getDeclaredField("encryptionKey");
        field.setAccessible(true);
        field.set(service, key);
    }

    private void mockProfiles(String... profiles) {
        when(environment.getActiveProfiles()).thenReturn(profiles);
    }

    private static String base64Key(int byteLength) {
        byte[] bytes = new byte[byteLength];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i + 1);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** 生成与 base64Key 不同的 32 字节密钥（错位填充） */
    private static String differentKey() {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (255 - i);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Nested
    @DisplayName("init 密钥初始化测试")
    class InitTests {

        @Test
        @DisplayName("配置有效 256 位密钥 — 加密解密成功")
        void init_validKey_roundTripWorks() throws Exception {
            setEncryptionKey(base64Key(32));

            service.init();

            String encrypted = service.encrypt("secret-data");
            assertThat(encrypted).isNotEqualTo("secret-data");
            assertThat(service.decrypt(encrypted)).isEqualTo("secret-data");
        }

        @Test
        @DisplayName("占位符未解析（${...}）+ 开发环境 — 使用临时密钥")
        void init_placeholderKey_devUsesTemporary() throws Exception {
            setEncryptionKey("${gateway.security.encryption-key}");
            mockProfiles("dev");

            assertThatCode(service::init).doesNotThrowAnyException();

            String encrypted = service.encrypt("x");
            assertThat(service.decrypt(encrypted)).isEqualTo("x");
        }

        @Test
        @DisplayName("空白密钥 + 开发环境 — 使用临时密钥")
        void init_blankKey_devUsesTemporary() throws Exception {
            setEncryptionKey("  ");
            mockProfiles("local");

            assertThatCode(service::init).doesNotThrowAnyException();

            assertThat(service.decrypt(service.encrypt("y"))).isEqualTo("y");
        }

        @Test
        @DisplayName("生产环境无密钥 — 抛 ENCRYPTION_KEY_MISSING")
        void init_noKey_prodThrows() throws Exception {
            setEncryptionKey(null);
            mockProfiles(); // 无任何 profile

            assertThatThrownBy(service::init)
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("ENCRYPTION_KEY_MISSING"));
        }

        @Test
        @DisplayName("密钥长度非 256 位 — 抛 ENCRYPTION_KEY_INVALID")
        void init_wrongLengthKey_throws() throws Exception {
            setEncryptionKey(base64Key(16)); // 128 位
            // 长度校验在 isDevelopmentEnvironment 之前抛出，无需 stub profiles

            assertThatThrownBy(service::init)
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("ENCRYPTION_KEY_INVALID"));
        }

        @Test
        @DisplayName("非法 Base64 + 生产环境 — 抛 ENCRYPTION_KEY_INVALID_FORMAT")
        void init_invalidBase64_prodThrows() throws Exception {
            setEncryptionKey("not-valid-base64!!");
            mockProfiles();

            assertThatThrownBy(service::init)
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("ENCRYPTION_KEY_INVALID_FORMAT"));
        }

        @Test
        @DisplayName("非法 Base64 + 开发环境 — 使用临时密钥")
        void init_invalidBase64_devUsesTemporary() throws Exception {
            setEncryptionKey("not-valid-base64!!");
            mockProfiles("test");

            assertThatCode(service::init).doesNotThrowAnyException();

            assertThat(service.decrypt(service.encrypt("z"))).isEqualTo("z");
        }

        @Test
        @DisplayName("开发环境 profile 变体均视为开发")
        void init_devProfiles_allTreatedAsDev() throws Exception {
            for (String profile : new String[]{"dev", "test", "local", "development"}) {
                service = new Aes256EncryptionService(environment);
                setEncryptionKey("bad!!");
                mockProfiles(profile);
                assertThatCode(service::init).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt 边界测试")
    class EncryptDecryptTests {

        @Test
        @DisplayName("encrypt(null) 返回 null")
        void encrypt_null_returnsNull() {
            assertThat(service.encrypt(null)).isNull();
        }

        @Test
        @DisplayName("decrypt(null) 返回 null")
        void decrypt_null_returnsNull() {
            assertThat(service.decrypt(null)).isNull();
        }

        @Test
        @DisplayName("未初始化直接加密 — 抛 ENCRYPTION_FAILED")
        void encrypt_withoutInit_throws() {
            // 未调用 init()，secretKey 为 null → 加密失败
            assertThatThrownBy(() -> service.encrypt("data"))
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("ENCRYPTION_FAILED"));
        }

        @Test
        @DisplayName("解密非法 Base64 — 抛 DECRYPTION_FAILED")
        void decrypt_invalidBase64_throws() {
            assertThatThrownBy(() -> service.decrypt("!!!not-base64!!!"))
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("DECRYPTION_FAILED"));
        }

        @Test
        @DisplayName("加密结果每次不同（随机 IV），解密一致")
        void encrypt_randomIv_decryptConsistent() throws Exception {
            setEncryptionKey(base64Key(32));
            service.init();

            String e1 = service.encrypt("same");
            String e2 = service.encrypt("same");

            assertThat(e1).isNotEqualTo(e2);
            assertThat(service.decrypt(e1)).isEqualTo("same");
            assertThat(service.decrypt(e2)).isEqualTo("same");
        }

        @Test
        @DisplayName("错误密钥无法解密 — 抛 DECRYPTION_FAILED")
        void decrypt_wrongKey_throws() throws Exception {
            setEncryptionKey(base64Key(32));
            service.init();
            String encrypted = service.encrypt("secret");

            // 换密钥后解密失败（GCM 认证失败）
            Aes256EncryptionService other = new Aes256EncryptionService(environment);
            Field field = Aes256EncryptionService.class.getDeclaredField("encryptionKey");
            field.setAccessible(true);
            field.set(other, differentKey());
            other.init();

            assertThatThrownBy(() -> other.decrypt(encrypted))
                    .isInstanceOf(IamException.class)
                    .satisfies(ex -> assertThat(((IamException) ex).getCode())
                            .isEqualTo("DECRYPTION_FAILED"));
        }
    }
}
