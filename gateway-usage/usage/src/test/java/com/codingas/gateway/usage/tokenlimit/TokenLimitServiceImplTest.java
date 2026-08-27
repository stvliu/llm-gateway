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
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.usage.tokenlimit.TokenLimitRepository;
import com.codingas.gateway.iam.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TokenLimitServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitServiceImpl 测试")
class TokenLimitServiceImplTest {

    @Mock
    private TokenLimitRepository tokenLimitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private TokenLimitServiceImpl service;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_validRequest_returnsCreated() {
            // given
            TokenLimit request = new TokenLimit();
request.setLimitType(TokenLimit.LimitType.USER_CUSTOM);
request.setMaxTokens(BigDecimal.valueOf(100000));
request.setPeriodType(PeriodType.MONTHLY);
request.setExceededAction(ExceededAction.REJECT);

            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tokenLimitRepository.save(any())).thenAnswer(inv -> {
                TokenLimit t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            // when
            TokenLimit result = service.create(1L, null, null, null, request);

            // then
            assertThat(result).isNotNull();
        }

        // 测试已移除重复代码检查，因为 limitCode 字段已被删除

        @Test
        @DisplayName("用户不存在抛出异常")
        void create_userNotFound_throwsException() {
            // given
            TokenLimit request = new TokenLimit();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(999L, null, null, null, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("创建限额同时绑定提供商、模型与切换模型")
        void create_withProviderModelSwitchModel_savesAllRefs() {
            // given
            TokenLimit request = new TokenLimit();
request.setMaxTokens(BigDecimal.valueOf(50000));

            User user = createTestUser();
            Provider provider = new Provider();
            provider.setId(10L);
            Model model = new Model();
            model.setId(20L);
            Model switchModel = new Model();
            switchModel.setId(30L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(modelRepository.findById(20L)).thenReturn(Optional.of(model));
            when(modelRepository.findById(30L)).thenReturn(Optional.of(switchModel));
            when(tokenLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            TokenLimit result = service.create(1L, 10L, 20L, 30L, request);

            // then
            assertThat(result.getProvider().getId()).isEqualTo(10L);
            assertThat(result.getProvider().getName()).isEqualTo(provider.getName());
            assertThat(result.getModel().getId()).isEqualTo(20L);
            assertThat(result.getSwitchModel().getId()).isEqualTo(30L);
        }

        @Test
        @DisplayName("提供商不存在抛出异常")
        void create_providerNotFound_throwsException() {
            // given
            TokenLimit request = new TokenLimit();

            when(userRepository.findById(1L)).thenReturn(Optional.of(createTestUser()));
            when(providerRepository.findById(10L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(1L, 10L, null, null, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider");
        }

        @Test
        @DisplayName("模型不存在抛出异常")
        void create_modelNotFound_throwsException() {
            // given
            TokenLimit request = new TokenLimit();

            when(userRepository.findById(1L)).thenReturn(Optional.of(createTestUser()));
            when(modelRepository.findById(20L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(1L, null, 20L, null, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model");
        }

        @Test
        @DisplayName("切换模型不存在抛出异常")
        void create_switchModelNotFound_throwsException() {
            // given
            TokenLimit request = new TokenLimit();

            when(userRepository.findById(1L)).thenReturn(Optional.of(createTestUser()));
            when(modelRepository.findById(30L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(1L, null, null, 30L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取 Token 限额成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));

            // when
            TokenLimit result = service.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Token 限额不存在抛出异常")
        void getById_notFound_throwsException() {
            // given
            when(tokenLimitRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询 Token 限额列表")
        void query_validRequest_returnsPage() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findAll()).thenReturn(List.of(tokenLimit));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setPage(1);
            request.setLimit(10);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按关键字过滤用户名")
        void query_keywordFilter_filtersByUsername() {
            // given
            TokenLimit matching = createTestTokenLimit();
            matching.setUser(createTestUser());
            TokenLimit other = createTestTokenLimit();
            User otherUser = createTestUser();
            otherUser.setUsername("another");
            other.setUser(otherUser);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(matching, other));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setKeyword("testuser");

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按 userId 过滤")
        void query_userIdFilter_filtersByUser() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findAll()).thenReturn(List.of(tokenLimit));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setUserId(1L);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);

            // 不匹配的用户 ID 应过滤掉全部
            request.setUserId(2L);
            result = service.query(request);
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("按 providerId 过滤（含 provider 为空的条目）")
        void query_providerIdFilter_filtersByProvider() {
            // given
            TokenLimit withProvider = createTestTokenLimit();
            Provider provider = new Provider();
            provider.setId(10L);
            withProvider.setProvider(provider);
            TokenLimit withoutProvider = createTestTokenLimit();
            withoutProvider.setId(2L);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(withProvider, withoutProvider));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setProviderId(10L);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按 modelId 过滤（含 model 为空的条目）")
        void query_modelIdFilter_filtersByModel() {
            // given
            TokenLimit withModel = createTestTokenLimit();
            Model model = new Model();
            model.setId(20L);
            withModel.setModel(model);
            TokenLimit withoutModel = createTestTokenLimit();
            withoutModel.setId(2L);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(withModel, withoutModel));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setModelId(20L);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按 state 过滤")
        void query_stateFilter_filtersByState() {
            // given
            TokenLimit active = createTestTokenLimit();
            TokenLimit suspended = createTestTokenLimit();
            suspended.setId(2L);
            suspended.setState(TokenLimit.TokenLimitState.SUSPENDED);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(active, suspended));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setState(TokenLimit.TokenLimitState.SUSPENDED);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("分页正确跳过和截取")
        void query_pagination_skipsAndLimits() {
            // given
            TokenLimit a = createTestTokenLimit();
            TokenLimit b = createTestTokenLimit();
            b.setId(2L);
            TokenLimit c = createTestTokenLimit();
            c.setId(3L);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(a, b, c));

            TokenLimitQuery request = new TokenLimitQuery();
            request.setPage(2);
            request.setLimit(1);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
            // page=2、limit=1 → 跳过第 1 条，返回第 2 条
            assertThat(result.getItems().get(0).getId()).isEqualTo(2L);
            assertThat(result.getPagination().getTotal()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新 Token 限额成功")
        void update_validRequest_returnsUpdated() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitRepository.save(any())).thenReturn(tokenLimit);

            TokenLimit request = new TokenLimit();
request.setMaxTokens(BigDecimal.valueOf(200000));

            // when
            TokenLimit result = service.update(1L, request, null);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("更新全字段并切换模型")
        void update_allFields_appliesAllChanges() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(modelRepository.findById(30L)).thenReturn(Optional.of(new Model()));
            when(tokenLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TokenLimit request = new TokenLimit();
request.setMaxTokens(BigDecimal.valueOf(300000));
request.setPeriodType(PeriodType.WEEKLY);
request.setPeriodDayOfWeek(1);
request.setPeriodDayOfMonth(5);
request.setExceededAction(ExceededAction.DOWNGRADE);
request.setState(true ? TokenLimit.TokenLimitState.ACTIVE : TokenLimit.TokenLimitState.SUSPENDED);

            // when
            TokenLimit result = service.update(1L, request, 30L);

            // then
            assertThat(result.getMaxTokens()).isEqualByComparingTo(BigDecimal.valueOf(300000));
            assertThat(tokenLimit.getPeriodType()).isEqualTo(PeriodType.WEEKLY);
            assertThat(tokenLimit.getPeriodDayOfWeek()).isEqualTo(1);
            assertThat(tokenLimit.getPeriodDayOfMonth()).isEqualTo(5);
            assertThat(tokenLimit.getExceededAction()).isEqualTo(ExceededAction.DOWNGRADE);
            assertThat(tokenLimit.getSwitchModel()).isNotNull();
            assertThat(tokenLimit.getState()).isEqualTo(TokenLimit.TokenLimitState.ACTIVE);
        }

        @Test
        @DisplayName("更新 enabled=false 置为暂停状态")
        void update_disabled_setsSuspended() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitRepository.save(any())).thenReturn(tokenLimit);

            TokenLimit request = new TokenLimit();
request.setState(false ? TokenLimit.TokenLimitState.ACTIVE : TokenLimit.TokenLimitState.SUSPENDED);

            // when
            TokenLimit result = service.update(1L, request, null);

            // then
            assertThat(tokenLimit.getState()).isEqualTo(TokenLimit.TokenLimitState.SUSPENDED);
            assertThat(result.getState()).isEqualTo(TokenLimit.TokenLimitState.SUSPENDED);
        }

        @Test
        @DisplayName("更新切换模型不存在抛出异常")
        void update_switchModelNotFound_throwsException() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(modelRepository.findById(30L)).thenReturn(Optional.empty());

            TokenLimit request = new TokenLimit();

            // when & then
            assertThatThrownBy(() -> service.update(1L, request, 30L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model");
        }

        @Test
        @DisplayName("更新不存在的限额抛出异常")
        void update_notFound_throwsException() {
            // given
            when(tokenLimitRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.update(999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 Token 限额成功")
        void delete_existingId_success() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitRepository.save(any())).thenReturn(tokenLimit);

            // when
            service.delete(1L);

            // then
            verify(tokenLimitRepository).save(any());
        }

        @Test
        @DisplayName("删除不存在的限额抛出异常")
        void delete_notFound_throwsException() {
            // given
            when(tokenLimitRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resetUsage 方法测试")
    class ResetUsageTests {

        @Test
        @DisplayName("重置使用量成功")
        void resetUsage_existingId_returnsUpdated() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            tokenLimit.setUsedTokens(BigDecimal.valueOf(50000));
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitRepository.save(any())).thenReturn(tokenLimit);

            // when
            TokenLimit result = service.resetUsage(1L);

            // then
            assertThat(result.getUsedTokens()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("重置不存在的限额抛出异常")
        void resetUsage_notFound_throwsException() {
            // given
            when(tokenLimitRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.resetUsage(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // Helper methods
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }

    private TokenLimit createTestTokenLimit() {
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setId(1L);
        tokenLimit.setUser(createTestUser());
        tokenLimit.setMaxTokens(BigDecimal.valueOf(100000));
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(PeriodType.MONTHLY);
        tokenLimit.setExceededAction(ExceededAction.REJECT);
        tokenLimit.setState(TokenLimit.TokenLimitState.ACTIVE);
        return tokenLimit;
    }
}
