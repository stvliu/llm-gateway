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
package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.apikey.UserApiKeyRepository;
import com.codingas.gateway.iam.encryption.ApiKeyEncryptor;
import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
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
import static org.mockito.Mockito.when;

/**
 * AuthenticationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService 测试")
class AuthenticationServiceTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private ApiKeyEncryptor encryptionService;

    @Mock
    private UserRepository userRepository;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userApiKeyRepository, encryptionService, userRepository);
    }

    @Nested
    @DisplayName("authenticateUser 方法测试")
    class AuthenticateUserTests {

        @Test
        @DisplayName("认证成功")
        void authenticate_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");
            // 身份携带 users.role 真实角色（默认 USER）
            User user = new User();
            user.setRole("USER");
            when(userRepository.findById(50L)).thenReturn(Optional.of(user));

            Identity result = service.authenticateUser("sk-abc1xxxxx");

            assertThat(result.userId()).isEqualTo(50L);
            assertThat(result.credentialId()).isEqualTo(100L);
            assertThat(result.role()).isEqualTo("USER");
            // 权限锚点 applicationId 必须从 UserApiKey 透传到 Identity
            assertThat(result.applicationId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("认证成功：身份携带真实用户角色（users.role）")
        void authenticate_validKey_returnsRealRole() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");
            User user = new User();
            user.setRole("ADMIN");
            when(userRepository.findById(50L)).thenReturn(Optional.of(user));

            Identity identity = service.authenticateUser("sk-abc1xxxxx");

            assertThat(identity.role()).isEqualTo("ADMIN");
            assertThat(identity.userId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("用户已删除：角色为 null（数据面不消费 role，无影响）")
        void authenticate_userDeleted_roleIsNull() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");
            // findById 返回 empty：用户已删除，角色为 null
            when(userRepository.findById(50L)).thenReturn(Optional.empty());

            Identity identity = service.authenticateUser("sk-abc1xxxxx");

            assertThat(identity.role()).isNull();
            assertThat(identity.userId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("API Key 为空 — 抛异常")
        void authenticate_emptyKey() {
            assertThatThrownBy(() -> service.authenticateUser(""))
                    .isInstanceOf(AuthenticationFailedException.class);
            assertThatThrownBy(() -> service.authenticateUser(null))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("Key prefix 未找到 — 抛异常")
        void authenticate_prefixNotFound() {
            when(userApiKeyRepository.findByKeyPrefix("sk-unkno")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateUser("sk-unknown-key"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("无效的 API Key");
        }

        @Test
        @DisplayName("Key hash 不匹配 — 抛异常")
        void authenticate_hashMismatch() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("wrong-hash");

            assertThatThrownBy(() -> service.authenticateUser("sk-abc1xxxxx"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("无效的 API Key");
        }

        @Test
        @DisplayName("Key 已删除 — 抛异常")
        void authenticate_keyDeleted() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setDeleted(true);
            when(userApiKeyRepository.findByKeyPrefix("sk-abc1x")).thenReturn(Optional.of(apiKey));
            when(encryptionService.hashKey("sk-abc1xxxxx")).thenReturn("hash123");

            assertThatThrownBy(() -> service.authenticateUser("sk-abc1xxxxx"))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("已禁用");
        }
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(100L);
        apiKey.setUserId(50L);
        apiKey.setApplicationId(7L);
        apiKey.setKeyHash("hash123");
        apiKey.setKeyPrefix("sk-abc1x");
        return apiKey;
    }
}
