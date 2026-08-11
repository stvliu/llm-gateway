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
package com.codingas.gateway.integration;

import com.codingas.gateway.application.proxy.ChatDispatchService;
import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.KeyFailoverInvoker;
import com.codingas.gateway.application.proxy.routing.CredentialResolver;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.support.ProviderSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 全链路集成测试基类
 *
 * <p>Mock 认证+路由等外部依赖，直接调用 ChatDispatchService 验证完整的七阶段调度链。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
public abstract class FullContextIntegrationTestBase {

    @Autowired
    protected ChatDispatchService chatDispatchService;

    @MockBean
    protected AuthenticationDomainService authenticationDomainService;

    @MockBean
    protected RoutingResolver routingResolver;

    @MockBean
    protected CredentialResolver credentialResolver;

    @MockBean
    protected ChannelFailoverInvoker channelFailoverInvoker;

    @MockBean
    protected KeyFailoverInvoker keyFailoverInvoker;

    @MockBean
    protected UpstreamClientRegistry upstreamClientRegistry;

    @MockBean
    protected ResilientClientFactory resilientClientFactory;

    @MockBean
    protected ProviderSimulator providerSimulator;

    @MockBean
    protected DomainEventPublisher domainEventPublisher;

    @MockBean
    protected AuditGateway auditGateway;

    // ---------- 默认 Mock 行为 ----------

    /**
     * 初始化默认 Mock 行为
     *
     * <p>子类可重写此方法并调用 super.setupDefaultMocks() 后覆盖特定 Mock。</p>
     */
    @BeforeEach
    protected void setupDefaultMocks() {
        // 认证成功：返回一个默认 Identity（含 applicationId 权限锚点）
        when(authenticationDomainService.authenticateUser(anyString()))
                .thenReturn(Identity.of(1L, "user", 100L, 7L));

        // 路由解析：返回一个默认 RoutingContext
        when(routingResolver.resolve(anyString(), any(Protocol.class), anyLong(), anyLong(), anyString(), any(RoutingStrategy.class)))
                .thenReturn(new RoutingContext(
                        1L,
                        1L,
                        "http://localhost:8080",
                        Protocol.OPENAI,
                        "sk-test-key",
                        30,
                        false,
                        "gpt-4",
                        null,
                        FailureStrategy.FAIL_RETRY
                ));

        // 凭证解析：返回一个默认 API Key
        when(credentialResolver.resolve(anyLong()))
                .thenReturn("sk-test-key");
    }

    /**
     * 创建一个简单的 OpenAI 聊天请求
     */
    protected OpenAIChatRequest createSimpleChatRequest() {
        return OpenAIChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        new OpenAIChatRequest.Message("user", "Hello", null, null, null)
                ))
                .build();
    }
}
