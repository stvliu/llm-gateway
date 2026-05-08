package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.provider.ProviderService;
import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Provider;
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
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderController 测试")
class ProviderControllerTest {

    @Mock
    private ProviderService providerService;

    @InjectMocks
    private ProviderController controller;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建提供商成功")
        void create_validRequest_returnsCreated() {
            // given
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderCode("openai");

            ProviderResponse response = createTestResponse();
            when(providerService.create(any())).thenReturn(response);

            // when
            ProviderResponse result = controller.create(request);

            // then
            assertThat(result.getProviderCode()).isEqualTo("openai");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取提供商详情成功")
        void getById_existingId_returnsProvider() {
            // given
            ProviderResponse response = createTestResponse();
            when(providerService.getById(1L)).thenReturn(response);

            // when
            ProviderResponse result = controller.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询提供商列表")
        void query_validRequest_returnsPage() {
            // given
            ProviderResponse response = createTestResponse();
            PageResponse<ProviderResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(providerService.query(any(ProviderQueryRequest.class))).thenReturn(pageResponse);

            // when
            PageResponse<ProviderResponse> result = controller.query(new ProviderQueryRequest());

            // then
            assertThat(result.getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商成功")
        void update_validRequest_returnsUpdated() {
            // given
            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("Updated Name");

            ProviderResponse response = createTestResponse();
            when(providerService.update(eq(1L), any())).thenReturn(response);

            // when
            ProviderResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(providerService).delete(1L);

            // when
            controller.delete(1L);

            // then - void 方法，无返回值验证
        }
    }

    @Nested
    @DisplayName("setEnabled 方法测试")
    class SetEnabledTests {

        @Test
        @DisplayName("启用提供商")
        void setEnabled_enable_returnsUpdated() {
            // given
            ProviderResponse response = createTestResponse();
            response.setStatus(Provider.ProviderStatus.ACTIVE);
            when(providerService.setEnabled(1L, true)).thenReturn(response);

            // when
            ProviderResponse result = controller.setEnabled(1L, true);

            // then
            assertThat(result.getStatus()).isEqualTo(Provider.ProviderStatus.ACTIVE);
        }

        @Test
        @DisplayName("禁用提供商")
        void setEnabled_disable_returnsUpdated() {
            // given
            ProviderResponse response = createTestResponse();
            response.setStatus(Provider.ProviderStatus.SUSPENDED);
            when(providerService.setEnabled(1L, false)).thenReturn(response);

            // when
            ProviderResponse result = controller.setEnabled(1L, false);

            // then
            assertThat(result.getStatus()).isEqualTo(Provider.ProviderStatus.SUSPENDED);
        }
    }

    // Helper methods
    private ProviderResponse createTestResponse() {
        ProviderResponse response = new ProviderResponse();
        response.setId(1L);
        response.setProviderCode("openai");
        response.setProviderName("OpenAI");
        response.setProviderType(ProviderType.OPENAI);
        response.setBaseUrl("https://api.openai.com");
        response.setPriority(100);
        response.setStatus(Provider.ProviderStatus.ACTIVE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
