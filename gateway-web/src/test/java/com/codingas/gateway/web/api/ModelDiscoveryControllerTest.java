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
import com.codingas.gateway.provider.model.ModelDiscoveryService;
import com.codingas.gateway.web.api.dto.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.iam.auth.Identity;
import jakarta.servlet.http.HttpServletRequest;
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
    private List<Model> sampleModels;

    @BeforeEach
    void setUp() {
        identity = Identity.of(1L, "user", 1L, APPLICATION_ID);
        sampleModels = List.of(model("gpt-4", 1700000000L), model("gpt-3.5-turbo", 1700000001L));
    }

    private Model model(String name, long created) {
        Model m = new Model();
        m.setModelName(name);
        m.setCreatedAt(Instant.ofEpochSecond(created));
        return m;
    }

    @Nested
    @DisplayName("listModels 方法测试")
    class ListModelsTests {

        @Test
        @DisplayName("正常认证请求返回可见模型列表")
        void listModels_withValidIdentity_returnsModels() {
            // given
            when(request.getAttribute("identity")).thenReturn(identity);
            when(modelDiscoveryService.getVisibleModels(APPLICATION_ID)).thenReturn(sampleModels);

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
