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
package com.codingas.gateway.domain.iam.service;

import com.codingas.gateway.domain.iam.gateway.EncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ApiKeyEncryptionDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyEncryptionDomainService 测试")
class ApiKeyEncryptionDomainServiceTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private ApiKeyEncryptionDomainService service;

    @Nested
    @DisplayName("encrypt 方法测试")
    class EncryptTests {

        @Test
        @DisplayName("加密成功")
        void encrypt_validInput_returnsEncrypted() {
            when(encryptionService.encrypt("secret-key")).thenReturn("encrypted-value");

            String result = service.encrypt("secret-key");

            assertThat(result).isEqualTo("encrypted-value");
            verify(encryptionService).encrypt("secret-key");
        }

        @Test
        @DisplayName("null 输入抛出异常")
        void encrypt_nullInput_throwsException() {
            assertThatThrownBy(() -> service.encrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("空白输入抛出异常")
        void encrypt_blankInput_throwsException() {
            assertThatThrownBy(() -> service.encrypt("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("decrypt 方法测试")
    class DecryptTests {

        @Test
        @DisplayName("解密成功")
        void decrypt_validInput_returnsDecrypted() {
            when(encryptionService.decrypt("encrypted-value")).thenReturn("secret-key");

            String result = service.decrypt("encrypted-value");

            assertThat(result).isEqualTo("secret-key");
            verify(encryptionService).decrypt("encrypted-value");
        }

        @Test
        @DisplayName("null 输入抛出异常")
        void decrypt_nullInput_throwsException() {
            assertThatThrownBy(() -> service.decrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("空白输入抛出异常")
        void decrypt_blankInput_throwsException() {
            assertThatThrownBy(() -> service.decrypt("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("isValidKeyFormat 方法测试")
    class IsValidKeyFormatTests {

        @Test
        @DisplayName("有效 sk- 前缀格式")
        void isValidKeyFormat_skPrefix_returnsTrue() {
            assertThat(service.isValidKeyFormat("sk-1234567890")).isTrue();
        }

        @Test
        @DisplayName("sk-ant- 前缀格式")
        void isValidKeyFormat_skAntPrefix_returnsTrue() {
            assertThat(service.isValidKeyFormat("sk-ant-api03-xxxxx")).isTrue();
        }

        @Test
        @DisplayName("无效前缀返回 false")
        void isValidKeyFormat_invalidPrefix_returnsFalse() {
            assertThat(service.isValidKeyFormat("pk-1234567890")).isFalse();
        }

        @Test
        @DisplayName("null 输入返回 false")
        void isValidKeyFormat_nullInput_returnsFalse() {
            assertThat(service.isValidKeyFormat(null)).isFalse();
        }

        @Test
        @DisplayName("空白输入返回 false")
        void isValidKeyFormat_blankInput_returnsFalse() {
            assertThat(service.isValidKeyFormat("   ")).isFalse();
        }

        @Test
        @DisplayName("sk- 前缀但太短返回 false")
        void isValidKeyFormat_shortKey_returnsFalse() {
            assertThat(service.isValidKeyFormat("sk-abc")).isFalse();
        }
    }

    @Nested
    @DisplayName("hashKey 方法测试")
    class HashKeyTests {

        @Test
        @DisplayName("生成哈希成功")
        void hashKey_validInput_returnsHash() {
            String result = service.hashKey("sk-test-key");

            assertThat(result).isNotNull();
            assertThat(result).hasSize(64);
        }

        @Test
        @DisplayName("相同输入生成相同哈希")
        void hashKey_sameInput_sameHash() {
            String hash1 = service.hashKey("sk-test-key");
            String hash2 = service.hashKey("sk-test-key");

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("不同输入生成不同哈希")
        void hashKey_differentInput_differentHash() {
            String hash1 = service.hashKey("sk-key-1");
            String hash2 = service.hashKey("sk-key-2");

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("null 输入抛出异常")
        void hashKey_nullInput_throwsException() {
            assertThatThrownBy(() -> service.hashKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("空白输入抛出异常")
        void hashKey_blankInput_throwsException() {
            assertThatThrownBy(() -> service.hashKey("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }
    }
}
