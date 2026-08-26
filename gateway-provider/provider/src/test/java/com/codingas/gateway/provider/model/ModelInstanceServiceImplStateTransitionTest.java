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

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ModelInstanceManagerImpl 状态转换测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelInstanceManagerImpl 状态转换测试")
class ModelInstanceManagerImplStateTransitionTest {

    @Mock
    private ModelInstanceRepository modelInstanceRepository;
    @Mock
    private ModelRepository modelRepository;

    @Captor
    private ArgumentCaptor<ModelInstance> instanceCaptor;

    private ModelInstanceManagerImpl service;

    @BeforeEach
    void setUp() {
        service = new ModelInstanceManagerImpl(modelInstanceRepository, modelRepository);
    }

    private ModelInstance createInstance(Long id, Long channelId, ModelInstance.State state) {
        ModelInstance instance = new ModelInstance();
        instance.setId(id);
        instance.setChannelId(channelId);
        instance.setState(state);
        return instance;
    }



    @Nested
    @DisplayName("PENDING→ACTIVE 测试")
    class PendingToActiveTests {

        @Test
        @DisplayName("激活成功")
        void activate_success() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.PENDING);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            service.setEnabled(10L, 1L, "ACTIVE");

            verify(modelInstanceRepository).save(instanceCaptor.capture());
            assertThat(instanceCaptor.getValue().getState()).isEqualTo(ModelInstance.State.ACTIVE);
        }
    }

    @Nested
    @DisplayName("ACTIVE→SUSPENDED 测试")
    class ActiveToSuspendedTests {

        @Test
        @DisplayName("暂停成功")
        void suspend_success() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            service.setEnabled(10L, 1L, "SUSPENDED");

            verify(modelInstanceRepository).save(instanceCaptor.capture());
            assertThat(instanceCaptor.getValue().getState()).isEqualTo(ModelInstance.State.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("非法转换测试")
    class InvalidTransitionsTests {

        @Test
        @DisplayName("实例不存在时抛出异常")
        void instanceNotFound() {
            when(modelInstanceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setEnabled(10L, 99L, "ACTIVE"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("实例不属于该渠道时抛出异常")
        void channelMismatch() {
            ModelInstance instance = createInstance(1L, 20L, ModelInstance.State.ACTIVE);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, "SUSPENDED"))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_MISMATCH"));
        }

        @Test
        @DisplayName("RETIRED→任何状态非法")
        void retiredToAny_invalid() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.RETIRED);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, "ACTIVE"))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        }

        @Test
        @DisplayName("PENDING→SUSPENDED 非法转换被拒绝")
        void pendingToSuspended_invalid() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.PENDING);
            when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, "SUSPENDED"))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        }
    }
}
