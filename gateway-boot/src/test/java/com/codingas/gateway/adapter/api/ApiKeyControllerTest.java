package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.gatewayapikey.ApiKeyService;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * ApiKeyController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyController 测试")
class ApiKeyControllerTest {

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private ApiKeyController controller;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 API Key 成功")
        void create_validRequest_returnsCreated() {
            // given
            ApiKeyCreateRequest request = new ApiKeyCreateRequest();
            request.setUserId(1L);
            request.setName("Test Key");

            ApiKeyResponse response = createTestResponse();
            response.setRawKey("sk-test-xxxxx");
            when(apiKeyService.create(any())).thenReturn(response);

            // when
            ApiKeyResponse result = controller.create(request);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取 API Key 成功")
        void getById_existingId_returnsKey() {
            // given
            ApiKeyResponse response = createTestResponse();
            when(apiKeyService.getById(1L)).thenReturn(response);

            // when
            ApiKeyResponse result = controller.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询 API Key 列表")
        void query_validRequest_returnsPage() {
            // given
            ApiKeyResponse response = createTestResponse();
            PageResponse<ApiKeyResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(apiKeyService.query(any(ApiKeyQueryRequest.class))).thenReturn(pageResponse);

            // when
            PageResponse<ApiKeyResponse> result = controller.query(new ApiKeyQueryRequest());

            // then
            assertThat(result.getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新 API Key 成功")
        void update_validRequest_returnsUpdated() {
            // given
            ApiKeyUpdateRequest request = new ApiKeyUpdateRequest();
            request.setName("Updated Name");

            ApiKeyResponse response = createTestResponse();
            when(apiKeyService.update(eq(1L), any())).thenReturn(response);

            // when
            ApiKeyResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 API Key 成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(apiKeyService).delete(1L);

            // when
            controller.delete(1L);

            // then - void 方法，无返回值验证
        }
    }

    @Nested
    @DisplayName("setEnabled 方法测试")
    class SetEnabledTests {

        @Test
        @DisplayName("启用 API Key")
        void setEnabled_enable_returnsUpdated() {
            // given
            ApiKeyResponse response = createTestResponse();
            response.setState(GatewayApiKeyState.ACTIVE);
            when(apiKeyService.setEnabled(1L, true)).thenReturn(response);

            // when
            ApiKeyResponse result = controller.setEnabled(1L, true);

            // then
            assertThat(result.getState()).isEqualTo(GatewayApiKeyState.ACTIVE);
        }

        @Test
        @DisplayName("禁用 API Key")
        void setEnabled_disable_returnsUpdated() {
            // given
            ApiKeyResponse response = createTestResponse();
            response.setState(GatewayApiKeyState.DISABLED);
            when(apiKeyService.setEnabled(1L, false)).thenReturn(response);

            // when
            ApiKeyResponse result = controller.setEnabled(1L, false);

            // then
            assertThat(result.getState()).isEqualTo(GatewayApiKeyState.DISABLED);
        }
    }

    // Helper methods
    private ApiKeyResponse createTestResponse() {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setUsername("testuser");
        response.setName("Test Key");
        response.setState(GatewayApiKeyState.ACTIVE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
