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

import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelService;
import com.codingas.gateway.provider.model.ModelQuery;
import com.codingas.gateway.web.api.dto.*;
import com.codingas.gateway.web.advice.ApiResponseWrapperAdvice;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import com.codingas.gateway.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ModelController 单元测试
 *
 * <p>Controller 直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装；测试装配该 Advice，HTTP 契约断言走 {@code $.data.*}。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelController 测试")
class ModelControllerTest {

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone 装配：copy 端点走 HTTP 层验证 @Valid 校验（@NotBlank 缺失返回 400）；
        // 装配 ApiResponseWrapperAdvice（与生产一致，单对象响应包装为 $.data.*）与 GlobalExceptionHandler（校验失败转 400）
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiResponseWrapperAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建模型成功")
        void create_validRequest_returnsCreated() {
            // given
            ModelCreateRequest request = new ModelCreateRequest();

            Model model = createTestModel();
            when(modelService.create(any())).thenReturn(model);

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
            Model model = createTestModel();
            when(modelService.getById(1L)).thenReturn(model);

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
            Model model = createTestModel();
            PageResponse<Model> pageResponse = PageResponse.of(
                List.of(model), 1, 10, 1L
            );
            when(modelService.query(any(ModelQuery.class))).thenReturn(pageResponse);

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

            Model model = createTestModel();
            when(modelService.update(eq(1L), any())).thenReturn(model);

            // when
            ModelResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("unlock 方法测试")
    class UnlockTests {

        @Test
        @DisplayName("清除字段锁定成功")
        void unlock_existingId_returnsUpdated() {
            // given
            Model model = createTestModel();
            when(modelService.unlockFields(1L)).thenReturn(model);

            // when
            ModelResponse result = controller.unlockFields(1L);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
            verify(modelService).unlockFields(1L);
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
            Model model = createTestModel();
            when(modelService.setEnabled(1L, true)).thenReturn(model);

            // when
            ModelResponse result = controller.setEnabled(1L, true);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("禁用模型")
        void setEnabled_disable_returnsUpdated() {
            // given
            Model model = createTestModel();
            when(modelService.setEnabled(1L, false)).thenReturn(model);

            // when
            ModelResponse result = controller.setEnabled(1L, false);

            // then
            assertThat(result.getModelName()).isEqualTo("gpt-4");
        }
    }

    @Nested
    @DisplayName("copy 复制模型")
    class CopyTests {

        @Test
        @DisplayName("POST /api/v1/models/{id}/copy 返回新模型")
        void copy_returnsNewModel() throws Exception {
            // given：copy 返回新模型实体，Controller 再转换为 ModelResponse
            Model newModel = new Model();
            newModel.setId(2L);
            newModel.setModelName("gpt-4o");
            newModel.setDisplayName("GPT-4o");
            newModel.setModelFamily("openai");
            when(modelService.copy(eq(1L), any(Model.class))).thenReturn(newModel);

            mockMvc.perform(post("/api/v1/models/{id}/copy", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"modelName\":\"gpt-4o\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2L))
                    .andExpect(jsonPath("$.data.modelName").value("gpt-4o"))
                    .andExpect(jsonPath("$.data.displayName").value("GPT-4o"))
                    .andExpect(jsonPath("$.data.modelFamily").value("openai"));
        }

        @Test
        @DisplayName("modelName 缺失返回 400")
        void copy_missingModelName_badRequest() throws Exception {
            mockMvc.perform(post("/api/v1/models/{id}/copy", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // Helper methods
    private Model createTestModel() {
        Model model = new Model();
        model.setId(1L);
        model.setModelName("gpt-4");
        model.setDisplayName("GPT-4");
        model.setContextWindow(8192);
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());
        return model;
    }
}
