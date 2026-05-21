package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.provider.ProviderService;
import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
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
 * ProviderController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderController 测试")
class ProviderControllerTest {

    @Mock
    private ProviderService providerService;

    @Mock
    private com.codingas.gateway.domain.model.gateway.ProviderGateway providerGateway;

    private ProviderController controller;

    @BeforeEach
    void setUp() {
        controller = new ProviderController(providerService, providerGateway);
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建提供商成功")
        void create_validRequest_returnsCreated() {
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderName("OpenAI");

            ProviderResponse response = createTestResponse();
            when(providerService.create(any())).thenReturn(response);

            var result = controller.create(request);

            assertThat(result.getBody().getProviderName()).isEqualTo("OpenAI");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取提供商详情成功")
        void getById_existingId_returnsProvider() {
            ProviderResponse response = createTestResponse();
            when(providerService.getById(1L)).thenReturn(response);

            var result = controller.getById(1L);

            assertThat(result.getBody().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询提供商列表")
        void query_validRequest_returnsPage() {
            ProviderResponse response = createTestResponse();
            PageResponse<ProviderResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(providerService.query(any(ProviderQueryRequest.class))).thenReturn(pageResponse);

            var result = controller.query(new ProviderQueryRequest());

            assertThat(result.getBody().getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商成功")
        void update_validRequest_returnsUpdated() {
            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("Updated Name");

            ProviderResponse response = createTestResponse();
            when(providerService.update(eq(1L), any())).thenReturn(response);

            var result = controller.update(1L, request);

            assertThat(result.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商成功")
        void delete_existingId_returnsSuccess() {
            doNothing().when(providerService).delete(1L);

            controller.delete(1L);
        }
    }

    @Nested
    @DisplayName("setEnabled 方法测试")
    class SetEnabledTests {

        @Test
        @DisplayName("启用提供商")
        void setEnabled_enable_returnsUpdated() {
            ProviderResponse response = createTestResponse();
            response.setState("ACTIVE");
            when(providerService.setEnabled(1L, true)).thenReturn(response);

            var result = controller.setEnabled(1L, true);

            assertThat(result.getBody().getState()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("禁用提供商")
        void setEnabled_disable_returnsUpdated() {
            ProviderResponse response = createTestResponse();
            response.setState("DISABLED");
            when(providerService.setEnabled(1L, false)).thenReturn(response);

            var result = controller.setEnabled(1L, false);

            assertThat(result.getBody().getState()).isEqualTo("DISABLED");
        }
    }

    private ProviderResponse createTestResponse() {
        ProviderResponse response = new ProviderResponse();
        response.setId(1L);
        response.setProviderName("OpenAI");
        response.setPriority(100);
        response.setState("ACTIVE");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}