/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ModelInstanceStateTransitionRequest;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
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
 * ModelInstanceServiceImpl 状态转换测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelInstanceServiceImpl 状态转换测试")
class ModelInstanceServiceImplStateTransitionTest {

    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ModelGateway modelGateway;

    @Captor
    private ArgumentCaptor<ModelInstance> instanceCaptor;

    private ModelInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModelInstanceServiceImpl(modelInstanceGateway, modelGateway);
    }

    private ModelInstance createInstance(Long id, Long channelId, ModelInstance.State state) {
        ModelInstance instance = new ModelInstance();
        instance.setId(id);
        instance.setChannelId(channelId);
        instance.setState(state);
        return instance;
    }

    private ModelInstanceStateTransitionRequest request(String targetState) {
        ModelInstanceStateTransitionRequest req = new ModelInstanceStateTransitionRequest();
        req.setTargetState(targetState);
        return req;
    }

    @Nested
    @DisplayName("PENDING→ACTIVE 测试")
    class PendingToActiveTests {

        @Test
        @DisplayName("激活成功")
        void activate_success() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.PENDING);
            when(modelInstanceGateway.findById(1L)).thenReturn(Optional.of(instance));

            service.setEnabled(10L, 1L, request("ACTIVE"));

            verify(modelInstanceGateway).save(instanceCaptor.capture());
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
            when(modelInstanceGateway.findById(1L)).thenReturn(Optional.of(instance));

            service.setEnabled(10L, 1L, request("SUSPENDED"));

            verify(modelInstanceGateway).save(instanceCaptor.capture());
            assertThat(instanceCaptor.getValue().getState()).isEqualTo(ModelInstance.State.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("非法转换测试")
    class InvalidTransitionsTests {

        @Test
        @DisplayName("实例不存在时抛出异常")
        void instanceNotFound() {
            when(modelInstanceGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setEnabled(10L, 99L, request("ACTIVE")))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("实例不属于该渠道时抛出异常")
        void channelMismatch() {
            ModelInstance instance = createInstance(1L, 20L, ModelInstance.State.ACTIVE);
            when(modelInstanceGateway.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, request("SUSPENDED")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_MISMATCH"));
        }

        @Test
        @DisplayName("RETIRED→任何状态非法")
        void retiredToAny_invalid() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.RETIRED);
            when(modelInstanceGateway.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        }

        @Test
        @DisplayName("PENDING→SUSPENDED 非法转换被拒绝")
        void pendingToSuspended_invalid() {
            ModelInstance instance = createInstance(1L, 10L, ModelInstance.State.PENDING);
            when(modelInstanceGateway.findById(1L)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.setEnabled(10L, 1L, request("SUSPENDED")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        }
    }
}
