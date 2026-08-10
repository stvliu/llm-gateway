/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelService;
import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
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
 * ModelController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
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

            ModelResponse response = createTestResponse();
            when(modelService.create(any())).thenReturn(response);

            // when
            ModelResponse result = controller.create(request);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
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
            ModelResponse result = controller.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
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
            PageResponse<ModelResponse> result = controller.query(new ModelQueryRequest());

            // then
            assertThat(result.getItems()).hasSize(1);
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
            ModelResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
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
            controller.delete(1L);

            // then - void 方法，无返回值验证
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
            when(modelService.setEnabled(1L, true)).thenReturn(response);

            // when
            ModelResponse result = controller.setEnabled(1L, true);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("禁用模型")
        void setEnabled_disable_returnsUpdated() {
            // given
            ModelResponse response = createTestResponse();
            when(modelService.setEnabled(1L, false)).thenReturn(response);

            // when
            ModelResponse result = controller.setEnabled(1L, false);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
        }
    }

    // Helper methods
    private ModelResponse createTestResponse() {
        ModelResponse response = new ModelResponse();
        response.setId(1L);
        response.setModelName("gpt-4");
        response.setDisplayName("GPT-4");
        response.setContextWindow(8192);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
