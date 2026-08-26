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
package com.codingas.gateway.usage.tokenlimit;

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.usage.tokenlimit.TokenLimitRepository;
import com.codingas.gateway.iam.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TokenLimitManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitManager 单元测试")
class TokenLimitManagerTest {

    @Mock
    private TokenLimitRepository tokenLimitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private TokenLimitManagerImpl tokenLimitManager;

    private User testUser;
    private Provider testProvider;
    private Model testModel;
    private TokenLimit testTokenLimit;
    private TokenLimit tokenLimitEntity;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // 初始化测试提供商
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setName("OpenAI");

        // 初始化测试模型
        testModel = new Model();
        testModel.setId(1L);
        testModel.setDisplayName("GPT-4");

        // 初始化测试 Token 限额
        testTokenLimit = new TokenLimit();
        testTokenLimit.setId(1L);
        testTokenLimit.setUser(testUser);
        testTokenLimit.setProvider(testProvider);
        testTokenLimit.setModel(testModel);
        testTokenLimit.setLimitType(LimitType.USER_CUSTOM);
        testTokenLimit.setMaxTokens(new BigDecimal("10000"));
        testTokenLimit.setUsedTokens(BigDecimal.ZERO);
        testTokenLimit.setPeriodType(PeriodType.MONTHLY);
        testTokenLimit.setExceededAction(ExceededAction.REJECT);
        testTokenLimit.setState(TokenLimitState.ACTIVE);
        testTokenLimit.setCreatedAt(Instant.now());
        testTokenLimit.setUpdatedAt(Instant.now());

        // 初始化限额实体（业务字段；userId 等 ID 经 create 单独传入）
        tokenLimitEntity = new TokenLimit();
        tokenLimitEntity.setLimitType(LimitType.USER_CUSTOM);
        tokenLimitEntity.setMaxTokens(new BigDecimal("10000"));
        tokenLimitEntity.setPeriodType(PeriodType.MONTHLY);
        tokenLimitEntity.setExceededAction(ExceededAction.REJECT);
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateMethodTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
            when(modelRepository.findById(1L)).thenReturn(Optional.of(testModel));
            when(tokenLimitRepository.save(any(TokenLimit.class))).thenReturn(testTokenLimit);

            // when
            TokenLimit result = tokenLimitManager.create(1L, 1L, 1L, null, tokenLimitEntity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(result.getUser().getUsername()).isEqualTo(testUser.getUsername());
            assertThat(result.getProvider().getId()).isEqualTo(testProvider.getId());
            assertThat(result.getProvider().getName()).isEqualTo(testProvider.getName());
            assertThat(result.getModel().getId()).isEqualTo(testModel.getId());
            assertThat(result.getModel().getDisplayName()).isEqualTo(testModel.getDisplayName());
            assertThat(result.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(result.getMaxTokens().subtract(result.getUsedTokens()))
                    .isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(result.getState()).isEqualTo(TokenLimitState.ACTIVE);

            verify(userRepository).findById(1L);
            verify(providerRepository).findById(1L);
            verify(modelRepository).findById(1L);
            verify(tokenLimitRepository).save(any(TokenLimit.class));
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 用户不存在")
        void create_userNotFound_throwsResourceNotFoundException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitManager.create(1L, 1L, 1L, null, tokenLimitEntity))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(String.valueOf(1L));

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 提供商不存在")
        void create_providerNotFound_throwsResourceNotFoundException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(providerRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitManager.create(1L, 1L, 1L, null, tokenLimitEntity))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining(String.valueOf(1L));

            verify(userRepository).findById(1L);
            verify(providerRepository).findById(1L);
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 模型不存在")
        void create_modelNotFound_throwsResourceNotFoundException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
            when(modelRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitManager.create(1L, 1L, 1L, null, tokenLimitEntity))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining(String.valueOf(1L));

            verify(userRepository).findById(1L);
            verify(providerRepository).findById(1L);
            verify(modelRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdMethodTests {

        @Test
        @DisplayName("根据 ID 获取 Token 限额成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(testTokenLimit));

            // when
            TokenLimit result = tokenLimitManager.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testTokenLimit.getId());
            assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(result.getUser().getUsername()).isEqualTo(testUser.getUsername());
            assertThat(result.getProvider().getId()).isEqualTo(testProvider.getId());
            assertThat(result.getProvider().getName()).isEqualTo(testProvider.getName());
            assertThat(result.getModel().getId()).isEqualTo(testModel.getId());
            assertThat(result.getModel().getDisplayName()).isEqualTo(testModel.getDisplayName());
            assertThat(result.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(result.getUsedTokens()).isEqualByComparingTo(testTokenLimit.getUsedTokens());
            assertThat(result.getState()).isEqualTo(TokenLimitState.ACTIVE);

            verify(tokenLimitRepository).findById(1L);
        }

        @Test
        @DisplayName("根据 ID 获取 Token 限额失败 - 不存在")
        void getById_nonExistingId_throwsResourceNotFoundException() {
            // given
            when(tokenLimitRepository.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitManager.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("TokenLimit")
                .hasMessageContaining("99");

            verify(tokenLimitRepository).findById(99L);
        }
    }
}
