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

import com.codingas.gateway.usage.dto.TokenLimitCreateRequest;
import com.codingas.gateway.usage.dto.TokenLimitResponse;
import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.LimitType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.usage.tokenlimit.TokenLimitGateway;
import com.codingas.gateway.iam.user.UserGateway;
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
 * TokenLimitService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitService 单元测试")
class TokenLimitServiceTest {

    @Mock
    private TokenLimitGateway tokenLimitGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private TokenLimitServiceImpl tokenLimitService;

    private User testUser;
    private Provider testProvider;
    private Model testModel;
    private TokenLimit testTokenLimit;
    private TokenLimitCreateRequest createRequest;

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

        // 初始化创建请求
        createRequest = new TokenLimitCreateRequest();
        createRequest.setUserId(1L);
        createRequest.setProviderId(1L);
        createRequest.setModelId(1L);
        createRequest.setLimitType(LimitType.USER_CUSTOM);
        createRequest.setMaxTokens(new BigDecimal("10000"));
        createRequest.setPeriodType(PeriodType.MONTHLY);
        createRequest.setExceededAction(ExceededAction.REJECT);
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateMethodTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_success() {
            // given
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.of(testProvider));
            when(modelGateway.findById(createRequest.getModelId())).thenReturn(Optional.of(testModel));
            when(tokenLimitGateway.save(any(TokenLimit.class))).thenReturn(testTokenLimit);

            // when
            TokenLimitResponse response = tokenLimitService.create(createRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getProviderId()).isEqualTo(testProvider.getId());
            assertThat(response.getProviderName()).isEqualTo(testProvider.getName());
            assertThat(response.getModelId()).isEqualTo(testModel.getId());
            assertThat(response.getModelName()).isEqualTo(testModel.getDisplayName());
            assertThat(response.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getRemainingTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getEnabled()).isTrue();

            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
            verify(modelGateway).findById(createRequest.getModelId());
            verify(tokenLimitGateway).save(any(TokenLimit.class));
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 用户不存在")
        void create_userNotFound_throwsResourceNotFoundException() {
            // given
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitService.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(String.valueOf(createRequest.getUserId()));

            verify(userGateway).findById(createRequest.getUserId());
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 提供商不存在")
        void create_providerNotFound_throwsResourceNotFoundException() {
            // given
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitService.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining(String.valueOf(createRequest.getProviderId()));

            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 模型不存在")
        void create_modelNotFound_throwsResourceNotFoundException() {
            // given
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.of(testProvider));
            when(modelGateway.findById(createRequest.getModelId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitService.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining(String.valueOf(createRequest.getModelId()));

            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
            verify(modelGateway).findById(createRequest.getModelId());
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdMethodTests {

        @Test
        @DisplayName("根据 ID 获取 Token 限额成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(testTokenLimit));

            // when
            TokenLimitResponse response = tokenLimitService.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testTokenLimit.getId());
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getProviderId()).isEqualTo(testProvider.getId());
            assertThat(response.getProviderName()).isEqualTo(testProvider.getName());
            assertThat(response.getModelId()).isEqualTo(testModel.getId());
            assertThat(response.getModelName()).isEqualTo(testModel.getDisplayName());
            assertThat(response.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getUsedTokens()).isEqualByComparingTo(testTokenLimit.getUsedTokens());
            assertThat(response.getEnabled()).isTrue();

            verify(tokenLimitGateway).findById(1L);
        }

        @Test
        @DisplayName("根据 ID 获取 Token 限额失败 - 不存在")
        void getById_nonExistingId_throwsResourceNotFoundException() {
            // given
            when(tokenLimitGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("TokenLimit")
                .hasMessageContaining("99");

            verify(tokenLimitGateway).findById(99L);
        }
    }
}
