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
package com.codingas.gateway.resiliencedata.failover;

import com.codingas.gateway.resilience.failover.FailoverEvent;
import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaFailoverEventRepository 单元测试
 *
 * <p>验证转移事件实体的持久化网关行为：save（DO↔Entity 转换，含 errorType/decision 枚举↔字符串互转、
 * exhausted 布尔、occurredAt 时间）、findRecent（since/applicationId 过滤 + occurredAt 倒序）、
 * findExhausted（exhausted=true 过滤）的委派逻辑。</p>
 *
 * <p>参照同类 Gateway 实现测试的 mock Repository 范式，不连 H2，
 * 聚焦 DO↔Entity 字段转换与查询参数委派。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaFailoverEventRepository 测试")
class JpaFailoverEventRepositoryTest {

    @Mock
    private FailoverEventJpaRepository repository;

    @InjectMocks
    private JpaFailoverEventRepository gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存转移事件并回写转换结果（枚举→字符串、exhausted、occurredAt）")
        void save_validEntity_returnsSaved() {
            FailoverEvent entity = createTestEntity();
            FailoverEventDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            FailoverEvent result = gateway.save(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTraceId()).isEqualTo("trace-abc-123");
            assertThat(result.getApplicationId()).isEqualTo(7L);
            assertThat(result.getFromChannelId()).isEqualTo(10L);
            assertThat(result.getFromEndpointId()).isEqualTo(20L);
            assertThat(result.getToChannelId()).isEqualTo(11L);
            assertThat(result.getToEndpointId()).isEqualTo(21L);
            assertThat(result.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
            assertThat(result.getDecision()).isEqualTo(FailoverDecision.L1);
            assertThat(result.isExhausted()).isFalse();
            assertThat(result.getOccurredAt()).isEqualTo(Instant.parse("2026-06-22T10:00:00Z"));

            // 验证 DO 转换：枚举转字符串、布尔透传、审计字段透传
            ArgumentCaptor<FailoverEventDo> captor = ArgumentCaptor.forClass(FailoverEventDo.class);
            verify(repository).save(captor.capture());
            FailoverEventDo captured = captor.getValue();
            assertThat(captured.getTraceId()).isEqualTo("trace-abc-123");
            assertThat(captured.getApplicationId()).isEqualTo(7L);
            assertThat(captured.getFromChannelId()).isEqualTo(10L);
            assertThat(captured.getToChannelId()).isEqualTo(11L);
            assertThat(captured.getErrorType()).isEqualTo("AUTHENTICATION_ERROR");
            assertThat(captured.getDecision()).isEqualTo("L1");
            assertThat(captured.isExhausted()).isFalse();
            assertThat(captured.getOccurredAt()).isEqualTo(Instant.parse("2026-06-22T10:00:00Z"));
        }

        @Test
        @DisplayName("save exhausted=true 与 L1 决策正确转换")
        void save_exhaustedL1_convertsCorrectly() {
            FailoverEvent entity = createTestEntity();
            entity.setDecision(FailoverDecision.L1);
            entity.setExhausted(true);
            entity.setToChannelId(null);
            entity.setToEndpointId(null);

            FailoverEventDo savedDo = createTestDo();
            savedDo.setDecision("L1");
            savedDo.setExhausted(true);
            savedDo.setToChannelId(null);
            savedDo.setToEndpointId(null);
            when(repository.save(any())).thenReturn(savedDo);

            FailoverEvent result = gateway.save(entity);

            ArgumentCaptor<FailoverEventDo> captor = ArgumentCaptor.forClass(FailoverEventDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getDecision()).isEqualTo("L1");
            assertThat(captor.getValue().isExhausted()).isTrue();
            assertThat(result.getDecision()).isEqualTo(FailoverDecision.L1);
            assertThat(result.isExhausted()).isTrue();
        }

        @Test
        @DisplayName("读取历史 decision=L2 容错为 NONE 不抛异常（Task 4 已删 L2 枚举值）")
        void save_legacyL2Decision_fallsBackToNone() {
            FailoverEvent entity = createTestEntity();
            FailoverEventDo savedDo = createTestDo();
            // 模拟数据库历史行残留 decision='L2'（Task 4 已从枚举移除）
            savedDo.setDecision("L2");
            when(repository.save(any())).thenReturn(savedDo);

            FailoverEvent result = gateway.save(entity);

            // 历史未知值 L2 容错为 NONE，不抛 IllegalArgumentException
            assertThat(result.getDecision()).isEqualTo(FailoverDecision.NONE);
        }

        @Test
        @DisplayName("读取未知 errorType 容错为 null 不抛异常")
        void save_unknownErrorType_fallsBackToNull() {
            FailoverEvent entity = createTestEntity();
            FailoverEventDo savedDo = createTestDo();
            // 模拟数据库历史行残留未知 errorType
            savedDo.setErrorType("BOGUS_TYPE");
            when(repository.save(any())).thenReturn(savedDo);

            FailoverEvent result = gateway.save(entity);

            // 未知 errorType 容错为 null，不抛 IllegalArgumentException
            assertThat(result.getErrorType()).isNull();
        }
    }

    @Nested
    @DisplayName("findRecent 方法测试")
    class FindRecentTests {

        @Test
        @DisplayName("仅 since 过滤：委派 Repository.findRecent 并还原枚举")
        void findRecent_bySince_returnsMapped() {
            Instant since = Instant.parse("2026-06-22T00:00:00Z");
            FailoverEventDo d1 = createTestDo();
            d1.setId(2L);
            d1.setOccurredAt(Instant.parse("2026-06-22T09:00:00Z"));
            FailoverEventDo d2 = createTestDo();
            d2.setId(1L);
            d2.setOccurredAt(Instant.parse("2026-06-22T08:00:00Z"));
            when(repository.findRecent(eq(since), eq(null), any(Pageable.class)))
                    .thenReturn(List.of(d1, d2));

            List<FailoverEvent> result = gateway.findRecent(since, null, 100);

            assertThat(result).hasSize(2);
            // Repository 已返回倒序，Gateway 仅做映射，顺序保持
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(1).getId()).isEqualTo(1L);
            assertThat(result.get(0).getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
            assertThat(result.get(0).getDecision()).isEqualTo(FailoverDecision.L1);
            // 验证 limit 转为 Pageable
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findRecent(eq(since), eq(null), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("applicationId 过滤：透传给 Repository")
        void findRecent_byApplicationId_delegatesFilter() {
            FailoverEventDo d = createTestDo();
            when(repository.findRecent(eq(null), eq(7L), any(Pageable.class)))
                    .thenReturn(List.of(d));

            List<FailoverEvent> result = gateway.findRecent(null, 7L, 50);

            assertThat(result).hasSize(1);
            verify(repository).findRecent(eq(null), eq(7L), any(Pageable.class));
        }

        @Test
        @DisplayName("全部参数为空：返回全量倒序（limit 截断）")
        void findRecent_allNull_delegatesWithNulls() {
            FailoverEventDo d = createTestDo();
            when(repository.findRecent(eq(null), eq(null), any(Pageable.class)))
                    .thenReturn(List.of(d));

            List<FailoverEvent> result = gateway.findRecent(null, null, 100);

            assertThat(result).hasSize(1);
            verify(repository).findRecent(eq(null), eq(null), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findExhausted 方法测试")
    class FindExhaustedTests {

        @Test
        @DisplayName("查询耗尽事件：exhausted=true，按 occurredAt 倒序")
        void findExhausted_returnsExhaustedEvents() {
            Instant since = Instant.parse("2026-06-22T00:00:00Z");
            FailoverEventDo d1 = createTestDo();
            d1.setId(2L);
            d1.setExhausted(true);
            d1.setOccurredAt(Instant.parse("2026-06-22T09:00:00Z"));
            FailoverEventDo d2 = createTestDo();
            d2.setId(1L);
            d2.setExhausted(true);
            d2.setOccurredAt(Instant.parse("2026-06-22T08:00:00Z"));
            when(repository.findExhausted(eq(since), any(Pageable.class)))
                    .thenReturn(List.of(d1, d2));

            List<FailoverEvent> result = gateway.findExhausted(since, 50);

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(e -> assertThat(e.isExhausted()).isTrue());
            verify(repository).findExhausted(eq(since), any(Pageable.class));
        }

        @Test
        @DisplayName("findExhausted since 为空时透传 null")
        void findExhausted_nullSince_delegatesNull() {
            FailoverEventDo d = createTestDo();
            d.setExhausted(true);
            when(repository.findExhausted(eq(null), any(Pageable.class))).thenReturn(List.of(d));

            List<FailoverEvent> result = gateway.findExhausted(null, 50);

            assertThat(result).hasSize(1);
            verify(repository).findExhausted(eq(null), any(Pageable.class));
        }
    }

    // ===== Helper methods =====

    private FailoverEventDo createTestDo() {
        FailoverEventDo d = new FailoverEventDo();
        d.setId(1L);
        d.setTraceId("trace-abc-123");
        d.setApplicationId(7L);
        d.setFromChannelId(10L);
        d.setFromEndpointId(20L);
        d.setToChannelId(11L);
        d.setToEndpointId(21L);
        d.setErrorType("AUTHENTICATION_ERROR");
        d.setDecision("L1");
        d.setExhausted(false);
        d.setOccurredAt(Instant.parse("2026-06-22T10:00:00Z"));
        return d;
    }

    private FailoverEvent createTestEntity() {
        FailoverEvent entity = new FailoverEvent();
        entity.setId(1L);
        entity.setTraceId("trace-abc-123");
        entity.setApplicationId(7L);
        entity.setFromChannelId(10L);
        entity.setFromEndpointId(20L);
        entity.setToChannelId(11L);
        entity.setToEndpointId(21L);
        entity.setErrorType(ProviderErrorType.AUTHENTICATION_ERROR);
        entity.setDecision(FailoverDecision.L1);
        entity.setExhausted(false);
        entity.setOccurredAt(Instant.parse("2026-06-22T10:00:00Z"));
        return entity;
    }
}
