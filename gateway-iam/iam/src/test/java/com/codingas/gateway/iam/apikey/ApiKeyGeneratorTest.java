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
package com.codingas.gateway.iam.apikey;

import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.apikey.UserApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyGenerator 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyGenerator 测试")
class ApiKeyGeneratorTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    private UserApiKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultUserApiKeyGenerator(userApiKeyRepository);
    }

    @Nested
    @DisplayName("generate 方法测试")
    class GenerateTests {

        @Test
        @DisplayName("生成成功")
        void generate_success() {
            when(userApiKeyRepository.findByKeyPrefix(anyString())).thenReturn(Optional.empty());

            GeneratedApiKey result = generator.generate();

            assertThat(result.plainKey()).startsWith("sk-");
            assertThat(result.keyPrefix()).hasSize(10);
            assertThat(result.keyPrefix()).startsWith("sk-");
        }

        @Test
        @DisplayName("prefix 碰撞时重试成功")
        void generate_collision_retrySuccess() {
            UserApiKey existing = new UserApiKey();
            when(userApiKeyRepository.findByKeyPrefix(anyString()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());

            GeneratedApiKey result = generator.generate();

            assertThat(result).isNotNull();
            assertThat(result.plainKey()).startsWith("sk-");
            verify(userApiKeyRepository, times(2)).findByKeyPrefix(anyString());
        }

        @Test
        @DisplayName("prefix 碰撞超过重试次数抛异常")
        void generate_collision_exceedRetries() {
            UserApiKey existing = new UserApiKey();
            when(userApiKeyRepository.findByKeyPrefix(anyString()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> generator.generate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成唯一的 API Key");

            verify(userApiKeyRepository, times(5)).findByKeyPrefix(anyString());
        }
    }
}
