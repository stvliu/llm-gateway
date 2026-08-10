/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelDiscoveryService;
import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ModelDiscoveryController 单元测试
 *
 * <p>Controller 直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDiscoveryController 测试")
class ModelDiscoveryControllerTest {

    @Mock
    private ModelDiscoveryService modelDiscoveryService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ModelDiscoveryController controller;

    private static final Long APPLICATION_ID = 100L;

    private Identity identity;
    private ModelDiscoveryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        identity = Identity.of(1L, "user", 1L, APPLICATION_ID);
        sampleResponse = new ModelDiscoveryResponse("list", List.of(
                new ModelDiscoveryResponse.ModelItem("gpt-4", "model", 1700000000L, "system"),
                new ModelDiscoveryResponse.ModelItem("gpt-3.5-turbo", "model", 1700000001L, "system")
        ));
    }

    @Nested
    @DisplayName("listModels 方法测试")
    class ListModelsTests {

        @Test
        @DisplayName("正常认证请求返回可见模型列表")
        void listModels_withValidIdentity_returnsModels() {
            // given
            when(request.getAttribute("identity")).thenReturn(identity);
            when(modelDiscoveryService.getVisibleModels(APPLICATION_ID)).thenReturn(sampleResponse);

            // when
            ModelDiscoveryResponse result = controller.listModels(request);

            // then
            assertThat(result.getObject()).isEqualTo("list");
            assertThat(result.getData()).hasSize(2);
            assertThat(result.getData().get(0).getId()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("identity 为 null 时抛出异常")
        void listModels_withoutIdentity_throwsException() {
            // given
            when(request.getAttribute("identity")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> controller.listModels(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("缺少认证信息");
        }

        @Test
        @DisplayName("applicationId 为 null 时抛出异常")
        void listModels_withNullApplicationId_throwsException() {
            // given
            Identity identityWithoutApp = new Identity(1L, "user", 1L, null);
            when(request.getAttribute("identity")).thenReturn(identityWithoutApp);

            // when & then
            assertThatThrownBy(() -> controller.listModels(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("缺少认证信息");
        }
    }
}
