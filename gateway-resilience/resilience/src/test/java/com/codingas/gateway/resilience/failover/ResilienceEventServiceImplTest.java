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
package com.codingas.gateway.resilience.failover;

import com.codingas.gateway.resilience.dto.FailoverEventResponse;
import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResilienceEventServiceImpl 单元测试
 *
 * <p>验证应用服务层查询委派逻辑，重点守护 I2 修复：{@code findExhausted} 当 since 为 null 时
 * 由 Service 层补默认窗口（最近 1 小时），避免透传 null 返回全量历史耗尽事件导致前端总览页
 * 告警区被陈旧数据淹没。设计 D12「耗尽告警」语义为近期告警，默认最近 1 小时。</p>
 *
 * <p>findRecent 不补默认窗口（转移事件流需支持回溯历史，since 为 null 时按 limit 截断全量倒序）。</p>
 *
 * <p>用 {@link Strictness#LENIENT}：RED 阶段 Service 透传 null 时 stubbing 参数与实际调用不匹配，
 * strict 模式会报 PotentialStubbingProblem 干扰 RED 观察；lenient 让 RED 聚焦于断言失败本身。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResilienceEventServiceImpl 测试")
class ResilienceEventServiceImplTest {

    @Mock
    private FailoverEventRepository failoverEventRepository;

    @InjectMocks
    private ResilienceEventServiceImpl service;

    /**
     * RED（修复前）：{@code findExhausted(null, limit)} 透传 null 给 Gateway，断言 since 非 null 失败。
     * GREEN（修复后）：Service 层 since 为 null 时补最近 1 小时，传给 Gateway 的 since 落在
     * callTime 前 1 小时附近（容差 1 分钟防时钟抖动）。
     */
    @Test
    @DisplayName("findExhausted since 为 null 时补默认窗口最近 1 小时")
    void findExhausted_nullSince_appliesDefaultWindowOfOneHour() {
        Instant callTime = Instant.now();
        FailoverEvent entity = buildExhaustedEvent();
        when(failoverEventRepository.findExhausted(any(), anyInt()))
                .thenReturn(List.of(entity));

        service.findExhausted(null, 50);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(failoverEventRepository).findExhausted(sinceCaptor.capture(), anyInt());
        Instant capturedSince = sinceCaptor.getValue();
        // 默认窗口 = 最近 1 小时：capturedSince 应在 callTime 前 1 小时附近（容差 1 分钟防时钟抖动）
        assertThat(capturedSince).isNotNull();
        Duration elapsed = Duration.between(capturedSince, callTime);
        assertThat(elapsed.toMinutes()).isBetween(59L, 61L);
    }

    @Test
    @DisplayName("findExhausted since 非 null 时透传原值（不覆盖用户指定窗口）")
    void findExhausted_nonNullSince_delegatesOriginalValue() {
        Instant since = Instant.parse("2026-06-22T00:00:00Z");
        when(failoverEventRepository.findExhausted(any(), anyInt()))
                .thenReturn(List.of());

        service.findExhausted(since, 30);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(failoverEventRepository).findExhausted(sinceCaptor.capture(), anyInt());
        assertThat(sinceCaptor.getValue()).isEqualTo(since);
    }

    @Test
    @DisplayName("findRecent since 为 null 时透传 null（转移事件流支持回溯历史，不补默认窗口）")
    void findRecent_nullSince_delegatesNull() {
        when(failoverEventRepository.findRecent(any(), any(), anyInt()))
                .thenReturn(List.of());

        service.findRecent(null, null, 100);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(failoverEventRepository).findRecent(sinceCaptor.capture(), any(), anyInt());
        assertThat(sinceCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("findExhausted 返回的实体正确转为响应 DTO（枚举转字符串）")
    void findExhausted_mapsEntityToResponse() {
        FailoverEvent entity = buildExhaustedEvent();
        when(failoverEventRepository.findExhausted(any(), anyInt()))
                .thenReturn(List.of(entity));

        List<FailoverEventResponse> result = service.findExhausted(null, 50);

        assertThat(result).hasSize(1);
        FailoverEventResponse resp = result.get(0);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.isExhausted()).isTrue();
        assertThat(resp.getErrorType()).isEqualTo("AUTHENTICATION_ERROR");
        assertThat(resp.getDecision()).isEqualTo("L1");
    }

    private FailoverEvent buildExhaustedEvent() {
        FailoverEvent entity = new FailoverEvent();
        entity.setId(1L);
        entity.setTraceId("trace-x");
        entity.setApplicationId(7L);
        entity.setFromChannelId(10L);
        entity.setFromEndpointId(20L);
        entity.setToChannelId(null);
        entity.setToEndpointId(null);
        entity.setErrorType(ProviderErrorType.AUTHENTICATION_ERROR);
        entity.setDecision(FailoverDecision.L1);
        entity.setExhausted(true);
        entity.setOccurredAt(Instant.parse("2026-06-22T10:00:00Z"));
        return entity;
    }
}
