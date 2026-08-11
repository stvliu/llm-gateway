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
package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FailoverEventRepository 集成测试（连 H2）
 *
 * <p>验证 {@code findExhausted} 的真实 SQL 过滤逻辑（exhausted=true）。
 * Mockito 单测（{@link FailoverEventGatewayImplTest}）只验证参数透传，
 * 无法验证 Repository @Query 的 SQL 过滤逻辑真实生效。本测试连 H2 执行真实 SQL，
 * 消除 Mockito 盲区。</p>
 *
 * <p>参照 {@code ChannelHealthRepositoryIT} 的 {@code @SpringBootTest} + H2 范式
 * （application-test.yml：H2 PostgreSQL 兼容模式 + ddl-auto create-drop + flyway disabled，
 * 表由 JPA 实体自动建）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FailoverEventRepository 集成测试（H2）")
class FailoverEventRepositoryTest {

    @Autowired
    private FailoverEventGateway failoverEventGateway;

    /**
     * findExhausted 真实 SQL 过滤：仅返回 exhausted=true 的事件
     */
    @Test
    @DisplayName("findExhausted 仅返回 exhausted=true 的事件")
    void findExhausted_returnsOnlyExhausted() {
        FailoverEvent exhausted = buildEvent("trace-ex", 10L, null);
        exhausted.setExhausted(true);
        exhausted.setToChannelId(null);
        exhausted.setToEndpointId(null);
        exhausted.setDecision(FailoverDecision.L1);
        FailoverEvent normal = buildEvent("trace-norm", 10L, 20L);
        normal.setExhausted(false);
        failoverEventGateway.save(exhausted);
        failoverEventGateway.save(normal);

        List<FailoverEvent> result = failoverEventGateway.findExhausted(null, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTraceId()).isEqualTo("trace-ex");
        assertThat(result.get(0).isExhausted()).isTrue();
    }

    /**
     * 构造测试转移事件
     *
     * @param traceId       Trace ID
     * @param fromChannelId 失败渠道 ID
     * @param toChannelId   目标渠道 ID（null 表示耗尽）
     */
    private FailoverEvent buildEvent(String traceId, Long fromChannelId, Long toChannelId) {
        FailoverEvent e = new FailoverEvent();
        e.setTraceId(traceId);
        e.setApplicationId(7L);
        e.setFromChannelId(fromChannelId);
        e.setFromEndpointId(20L);
        e.setToChannelId(toChannelId);
        e.setToEndpointId(toChannelId != null ? 21L : null);
        e.setErrorType(ProviderErrorType.AUTHENTICATION_ERROR);
        e.setDecision(FailoverDecision.L1);
        e.setExhausted(false);
        e.setOccurredAt(Instant.now());
        return e;
    }
}
