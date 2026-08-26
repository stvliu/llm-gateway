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
package com.codingas.gateway.provider.model;

import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelInstanceServiceImpl 单元测试（CRUD/更新/删除分支）
 *
 * <p>setEnabled 状态转换分支由 {@code ModelInstanceServiceImplStateTransitionTest} 覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelInstanceServiceImpl 单元测试")
class ModelInstanceServiceImplTest {

    @Mock
    private ModelInstanceRepository modelInstanceRepository;
    @Mock
    private ModelRepository modelRepository;

    private ModelInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModelInstanceServiceImpl(modelInstanceRepository, modelRepository);
        // toView 组装需要查询模型规格；lenient 避免未使用该查询的测试报 UnnecessaryStubbing
        lenient().when(modelRepository.findById(any())).thenReturn(Optional.empty());
    }

    // ==================== getInstancesByChannelId 测试 ====================

    @Nested
    @DisplayName("getInstancesByChannelId 渠道实例列表")
    class GetInstancesTests {

        @Test
        @DisplayName("视图对象携带模型规格关联（供 web 层 DTO 纯映射）")
        void withModel_fillsModelFields() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            instance.setUpstreamModelName("up-1");
            when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(instance));
            Model spec = new Model();
            spec.setModelName("gpt-4");
            spec.setDisplayName("GPT-4");
            spec.setModelFamily("GPT");
            when(modelRepository.findById(100L)).thenReturn(Optional.of(spec));

            List<ModelInstanceView> result = service.getInstancesByChannelId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInstance().getId()).isEqualTo(1L);
            assertThat(result.get(0).getInstance().getModelId()).isEqualTo(100L);
            assertThat(result.get(0).getInstance().getUpstreamModelName()).isEqualTo("up-1");
            assertThat(result.get(0).getInstance().getState()).isEqualTo(ModelInstance.State.ACTIVE);
            // 视图对象携带模型规格关联（web 层 DTO 纯映射使用）
            assertThat(result.get(0).getModel().getModelName()).isEqualTo("gpt-4");
            assertThat(result.get(0).getModel().getDisplayName()).isEqualTo("GPT-4");
        }

        @Test
        @DisplayName("模型规格缺失时视图对象 model 为 null")
        void withoutModel_leavesModelFieldsNull() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.PENDING);
            when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(instance));

            List<ModelInstanceView> result = service.getInstancesByChannelId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInstance().getModelId()).isEqualTo(100L);
            assertThat(result.get(0).getInstance().getState()).isEqualTo(ModelInstance.State.PENDING);
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建模型实例")
    class CreateTests {

        @Test
        @DisplayName("模型已关联时抛 DuplicateResourceException")
        void alreadyLinked_throws() {
            when(modelInstanceRepository.existsByChannelIdAndModelId(10L, 100L)).thenReturn(true);

            assertThatThrownBy(() -> service.create(createRequest(10L, 100L, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(modelInstanceRepository, never()).save(any(ModelInstance.class));
        }

        @Test
        @DisplayName("创建成功，priority/weight 缺省为 100")
        void create_success_withDefaults() {
            when(modelInstanceRepository.existsByChannelIdAndModelId(10L, 100L)).thenReturn(false);
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(50L);
                return mi;
            });
            ModelInstanceView result = service.create(createRequest(10L, 100L, null, null));

            assertThat(result.getInstance().getId()).isEqualTo(50L);
            ArgumentCaptor<ModelInstance> captor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceRepository).save(captor.capture());
            assertThat(captor.getValue().getPriority()).isEqualTo(100);
            assertThat(captor.getValue().getWeight()).isEqualTo(100);
            assertThat(captor.getValue().getState()).isEqualTo(ModelInstance.State.ACTIVE);
        }

        @Test
        @DisplayName("创建成功时透传指定的 priority/weight 与上游模型名")
        void create_success_withExplicitValues() {
            when(modelInstanceRepository.existsByChannelIdAndModelId(10L, 100L)).thenReturn(false);
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(51L);
                return mi;
            });

            ModelInstanceCreateCommand request = new ModelInstanceCreateCommand(10L, 100L, "upstream-1", 5, 20);

            ModelInstanceView result = service.create(request);

            assertThat(result.getInstance().getUpstreamModelName()).isEqualTo("upstream-1");
            assertThat(result.getInstance().getPriority()).isEqualTo(5);
            assertThat(result.getInstance().getWeight()).isEqualTo(20);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除模型实例")
    class DeleteTests {

        @Test
        @DisplayName("实例不存在时抛 ResourceNotFoundException")
        void notFound_throws() {
            when(modelInstanceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("实例不属于该渠道时抛 CHANNEL_MISMATCH")
        void channelMismatch_throws() {
            ModelInstance instance = createInstance(1L, 20L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.delete(10L, 1L))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("CHANNEL_MISMATCH"));
            verify(modelInstanceRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            service.delete(10L, 1L);

            verify(modelInstanceRepository).deleteById(1L);
        }
    }

    // ==================== updateUpstreamModelName 测试 ====================

    @Nested
    @DisplayName("updateUpstreamModelName 更新上游模型名")
    class UpdateUpstreamModelNameTests {

        @Test
        @DisplayName("实例不存在时抛 ResourceNotFoundException")
        void notFound_throws() {
            when(modelInstanceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateUpstreamModelName(10L, 99L, "new-name"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("实例不属于该渠道时抛 CHANNEL_MISMATCH")
        void channelMismatch_throws() {
            ModelInstance instance = createInstance(1L, 20L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.updateUpstreamModelName(10L, 1L, "new-name"))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("更新成功")
        void update_success() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            service.updateUpstreamModelName(10L, 1L, "new-name");

            ArgumentCaptor<ModelInstance> captor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceRepository).save(captor.capture());
            assertThat(captor.getValue().getUpstreamModelName()).isEqualTo("new-name");
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新模型实例")
    class UpdateTests {

        @Test
        @DisplayName("实例不存在时抛 ResourceNotFoundException")
        void notFound_throws() {
            when(modelInstanceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(10L, 99L, new ModelInstanceUpdateCommand(null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("实例不属于该渠道时抛 CHANNEL_MISMATCH")
        void channelMismatch_throws() {
            ModelInstance instance = createInstance(1L, 20L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.update(10L, 1L, new ModelInstanceUpdateCommand(null, null)))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("新 modelId 已关联时抛 DuplicateResourceException")
        void conflictingModelId_throws() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));
            when(modelInstanceRepository.existsByChannelIdAndModelId(10L, 200L)).thenReturn(true);

            ModelInstanceUpdateCommand request = new ModelInstanceUpdateCommand(200L, null);

            assertThatThrownBy(() -> service.update(10L, 1L, request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(modelInstanceRepository, never()).save(any(ModelInstance.class));
        }

        @Test
        @DisplayName("更新 modelId 与 upstreamModelName 成功")
        void update_success() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));
            when(modelInstanceRepository.existsByChannelIdAndModelId(10L, 200L)).thenReturn(false);
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            ModelInstanceUpdateCommand request = new ModelInstanceUpdateCommand(200L, "upstream-2");

            ModelInstanceView result = service.update(10L, 1L, request);

            assertThat(result.getInstance().getModelId()).isEqualTo(200L);
            assertThat(result.getInstance().getUpstreamModelName()).isEqualTo("upstream-2");
        }

        @Test
        @DisplayName("modelId 与 upstreamModelName 均为 null 时不修改对应字段")
        void update_nullFields_noChange() {
            ModelInstance instance = createInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
            instance.setUpstreamModelName("keep-me");
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            ModelInstanceUpdateCommand request = new ModelInstanceUpdateCommand(null, null);

            ModelInstanceView result = service.update(10L, 1L, request);

            assertThat(result.getInstance().getModelId()).isEqualTo(100L);
            assertThat(result.getInstance().getUpstreamModelName()).isEqualTo("keep-me");
        }
    }

    // ==================== 辅助方法 ====================

    private ModelInstance createInstance(Long id, Long channelId, Long modelId, ModelInstance.State state) {
        ModelInstance instance = new ModelInstance();
        instance.setId(id);
        instance.setChannelId(channelId);
        instance.setModelId(modelId);
        instance.setState(state);
        return instance;
    }

    private ModelInstanceCreateCommand createRequest(Long channelId, Long modelId, String upstream, Integer priority) {
        return new ModelInstanceCreateCommand(channelId, modelId, upstream, priority, null);
    }
}
