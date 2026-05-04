package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelService;
import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.model.entity.Model;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * ModelController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelController 测试")
class ModelControllerTest {

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelController controller;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建模型成功")
        void create_validRequest_returnsCreated() {
            // given
            ModelCreateRequest request = new ModelCreateRequest();
            request.setModelCode("gpt-4");

            ModelResponse response = createTestResponse();
            when(modelService.create(any())).thenReturn(response);

            // when
            var result = controller.create(request);

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getModelCode()).isEqualTo("gpt-4");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取模型详情成功")
        void getById_existingId_returnsModel() {
            // given
            ModelResponse response = createTestResponse();
            when(modelService.getById(1L)).thenReturn(response);

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
        @DisplayName("查询模型列表")
        void query_validRequest_returnsPage() {
            // given
            ModelResponse response = createTestResponse();
            PageResponse<ModelResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(modelService.query(any(ModelQueryRequest.class))).thenReturn(pageResponse);

            // when
            var result = controller.query(new ModelQueryRequest());

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新模型成功")
        void update_validRequest_returnsUpdated() {
            // given
            ModelUpdateRequest request = new ModelUpdateRequest();
            request.setDisplayName("Updated Model");

            ModelResponse response = createTestResponse();
            when(modelService.update(eq(1L), any())).thenReturn(response);

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
        @DisplayName("删除模型成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(modelService).delete(1L);

            // when
            var result = controller.delete(1L);

            // then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("setEnabled 方法测试")
    class SetEnabledTests {

        @Test
        @DisplayName("启用模型")
        void setEnabled_enable_returnsUpdated() {
            // given
            ModelResponse response = createTestResponse();
            response.setStatus(Model.ModelStatus.ACTIVE);
            when(modelService.setEnabled(1L, true)).thenReturn(response);

            // when
            var result = controller.setEnabled(1L, true);

            // then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("禁用模型")
        void setEnabled_disable_returnsUpdated() {
            // given
            ModelResponse response = createTestResponse();
            response.setStatus(Model.ModelStatus.DEPRECATED);
            when(modelService.setEnabled(1L, false)).thenReturn(response);

            // when
            var result = controller.setEnabled(1L, false);

            // then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // Helper methods
    private ModelResponse createTestResponse() {
        ModelResponse response = new ModelResponse();
        response.setId(1L);
        response.setModelCode("gpt-4");
        response.setProviderId(1L);
        response.setProviderName("OpenAI");
        response.setProviderCode("openai");
        response.setDisplayName("GPT-4");
        response.setContextWindow(8192);
        response.setInputPrice(BigDecimal.valueOf(0.03));
        response.setOutputPrice(BigDecimal.valueOf(0.06));
        response.setStatus(Model.ModelStatus.ACTIVE);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
