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
import com.codingas.gateway.usage.dto.TokenLimitQueryRequest;
import com.codingas.gateway.usage.dto.TokenLimitResponse;
import com.codingas.gateway.usage.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.usage.tokenlimit.TokenLimitGateway;
import com.codingas.gateway.iam.user.UserGateway;
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
    private TokenLimitGateway tokenLimitGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private TokenLimitServiceImpl service;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_validRequest_returnsCreated() {
            // given
            TokenLimitCreateRequest request = new TokenLimitCreateRequest();
            request.setUserId(1L);
            request.setMaxTokens(BigDecimal.valueOf(100000));
            request.setPeriodType(PeriodType.MONTHLY);
            request.setExceededAction(ExceededAction.REJECT);

            User user = createTestUser();
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(tokenLimitGateway.save(any())).thenAnswer(inv -> {
                TokenLimit t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            // when
            TokenLimitResponse result = service.create(request);

            // then
            assertThat(result).isNotNull();
        }

        // 测试已移除重复代码检查，因为 limitCode 字段已被删除

        @Test
        @DisplayName("用户不存在抛出异常")
        void create_userNotFound_throwsException() {
            // given
            TokenLimitCreateRequest request = new TokenLimitCreateRequest();
            request.setUserId(999L);

            when(userGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
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
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(tokenLimit));

            // when
            TokenLimitResponse result = service.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Token 限额不存在抛出异常")
        void getById_notFound_throwsException() {
            // given
            when(tokenLimitGateway.findById(999L)).thenReturn(Optional.empty());

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
            when(tokenLimitGateway.findAll()).thenReturn(List.of(tokenLimit));

            TokenLimitQueryRequest request = new TokenLimitQueryRequest();
            request.setPage(1);
            request.setLimit(10);

            // when
            var result = service.query(request);

            // then
            assertThat(result.getItems()).hasSize(1);
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
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitGateway.save(any())).thenReturn(tokenLimit);

            TokenLimitUpdateRequest request = new TokenLimitUpdateRequest();
            request.setMaxTokens(BigDecimal.valueOf(200000));

            // when
            TokenLimitResponse result = service.update(1L, request);

            // then
            assertThat(result).isNotNull();
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
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitGateway.save(any())).thenReturn(tokenLimit);

            // when
            service.delete(1L);

            // then
            verify(tokenLimitGateway).save(any());
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
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(tokenLimit));
            when(tokenLimitGateway.save(any())).thenReturn(tokenLimit);

            // when
            TokenLimitResponse result = service.resetUsage(1L);

            // then
            assertThat(result).isNotNull();
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
