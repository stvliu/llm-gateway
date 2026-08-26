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
package com.codingas.gateway.web.api;

import com.codingas.gateway.usage.tokenlimit.TokenLimitManager;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimitQuery;
import com.codingas.gateway.web.api.dto.TokenLimitCreateRequest;
import com.codingas.gateway.web.api.dto.TokenLimitQueryRequest;
import com.codingas.gateway.web.api.dto.TokenLimitResponse;
import com.codingas.gateway.web.api.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.iam.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * TokenLimitController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitController 测试")
class TokenLimitControllerTest {

    @Mock
    private TokenLimitManager tokenLimitManager;

    @InjectMocks
    private TokenLimitController controller;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_validRequest_returnsCreated() {
            // given
            TokenLimitCreateRequest request = new TokenLimitCreateRequest();

            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitManager.create(any(), any(), any(), any(), any())).thenReturn(tokenLimit);

            // when
            TokenLimitResponse result = controller.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取 Token 限额详情成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitManager.getById(1L)).thenReturn(tokenLimit);

            // when
            TokenLimitResponse result = controller.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
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
            PageResponse<TokenLimit> pageResponse = PageResponse.of(
                List.of(tokenLimit), 1, 10, 1L
            );
            when(tokenLimitManager.query(any(TokenLimitQuery.class))).thenReturn(pageResponse);

            // when
            PageResponse<TokenLimitResponse> result = controller.query(new TokenLimitQueryRequest());

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
            TokenLimitUpdateRequest request = new TokenLimitUpdateRequest();
            request.setMaxTokens(BigDecimal.valueOf(200000));

            TokenLimit tokenLimit = createTestTokenLimit();
            when(tokenLimitManager.update(eq(1L), any(), any())).thenReturn(tokenLimit);

            // when
            TokenLimitResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 Token 限额成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(tokenLimitManager).delete(1L);

            // when
            controller.delete(1L);

            // then - void 方法，无返回值验证
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
            tokenLimit.setUsedTokens(BigDecimal.ZERO);
            when(tokenLimitManager.resetUsage(1L)).thenReturn(tokenLimit);

            // when
            TokenLimitResponse result = controller.resetUsage(1L);

            // then
            assertThat(result.getUsedTokens()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // Helper methods
    private TokenLimit createTestTokenLimit() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setId(1L);
        tokenLimit.setUser(user);
        tokenLimit.setLimitType(TokenLimit.LimitType.USER_CUSTOM);
        tokenLimit.setMaxTokens(BigDecimal.valueOf(100000));
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(PeriodType.MONTHLY);
        tokenLimit.setExceededAction(ExceededAction.REJECT);
        tokenLimit.setState(TokenLimit.TokenLimitState.ACTIVE);
        tokenLimit.setCreatedAt(Instant.now());
        tokenLimit.setUpdatedAt(Instant.now());
        return tokenLimit;
    }
}
