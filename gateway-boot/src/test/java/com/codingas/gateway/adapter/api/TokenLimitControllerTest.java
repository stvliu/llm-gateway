package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.quota.TokenLimitService;
import com.codingas.gateway.application.quota.dto.TokenLimitCreateRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitQueryRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitResponse;
import com.codingas.gateway.application.quota.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.domain.usage.entity.TokenLimit;
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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitController 测试")
class TokenLimitControllerTest {

    @Mock
    private TokenLimitService tokenLimitService;

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
            request.setLimitCode("limit-001");

            TokenLimitResponse response = createTestResponse();
            when(tokenLimitService.create(any())).thenReturn(response);

            // when
            var result = controller.create(request);

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getLimitCode()).isEqualTo("limit-001");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取 Token 限额详情成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            TokenLimitResponse response = createTestResponse();
            when(tokenLimitService.getById(1L)).thenReturn(response);

            // when
            var result = controller.getById(1L);

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询 Token 限额列表")
        void query_validRequest_returnsPage() {
            // given
            TokenLimitResponse response = createTestResponse();
            PageResponse<TokenLimitResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(tokenLimitService.query(any(TokenLimitQueryRequest.class))).thenReturn(pageResponse);

            // when
            var result = controller.query(new TokenLimitQueryRequest());

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
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

            TokenLimitResponse response = createTestResponse();
            when(tokenLimitService.update(eq(1L), any())).thenReturn(response);

            // when
            var result = controller.update(1L, request);

            // then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 Token 限额成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(tokenLimitService).delete(1L);

            // when
            var result = controller.delete(1L);

            // then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("resetUsage 方法测试")
    class ResetUsageTests {

        @Test
        @DisplayName("重置使用量成功")
        void resetUsage_existingId_returnsUpdated() {
            // given
            TokenLimitResponse response = createTestResponse();
            response.setUsedTokens(BigDecimal.ZERO);
            when(tokenLimitService.resetUsage(1L)).thenReturn(response);

            // when
            var result = controller.resetUsage(1L);

            // then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // Helper methods
    private TokenLimitResponse createTestResponse() {
        TokenLimitResponse response = new TokenLimitResponse();
        response.setId(1L);
        response.setLimitCode("limit-001");
        response.setUserId(1L);
        response.setUsername("testuser");
        response.setLimitType(TokenLimit.LimitType.USER_CUSTOM);
        response.setMaxTokens(BigDecimal.valueOf(100000));
        response.setUsedTokens(BigDecimal.ZERO);
        response.setRemainingTokens(BigDecimal.valueOf(100000));
        response.setPeriodType(PeriodType.MONTHLY);
        response.setExceededAction(ExceededAction.REJECT);
        response.setStatus(TokenLimit.TokenLimitStatus.ACTIVE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
