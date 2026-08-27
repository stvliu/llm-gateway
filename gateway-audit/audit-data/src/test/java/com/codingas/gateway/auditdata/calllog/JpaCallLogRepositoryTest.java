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
package com.codingas.gateway.auditdata.calllog;

import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.DailyUsage;
import com.codingas.gateway.audit.ModelUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaCallLogRepository 单元测试
 *
 * <p>mock CallLogJpaRepository，覆盖 save 及实体↔数据对象双向转换全字段断言。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCallLogRepository 测试")
class JpaCallLogRepositoryTest {

    @Mock
    private CallLogJpaRepository repository;

    @InjectMocks
    private JpaCallLogRepository gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存调用日志并返回含 ID 的实体（全字段转换）")
        void save_validEntity_returnsSavedEntity() {
            // given
            CallLog entity = createTestEntity();
            CallLogDo savedDo = createTestDo();
            savedDo.setId(99L);
            when(repository.save(any(CallLogDo.class))).thenReturn(savedDo);

            // when
            CallLog result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
            assertEntityFields(result);
            verify(repository).save(any(CallLogDo.class));
        }

        @Test
        @DisplayName("保存后字段逐一对应（双向转换一致性）")
        void save_roundTrip_fieldsMatch() {
            // given
            CallLog entity = createTestEntity();
            when(repository.save(any(CallLogDo.class))).thenAnswer(invocation -> {
                CallLogDo arg = invocation.getArgument(0);
                arg.setId(1L);
                return arg;
            });

            // when
            CallLog result = gateway.save(entity);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertEntityFields(result);
        }
    }

    // Helper 方法
    private CallLog createTestEntity() {
        CallLog entity = new CallLog();
        entity.setTraceId("trace-abc");
        entity.setUserId(1L);
        entity.setModel("gpt-4o");
        entity.setChannelId(10L);
        entity.setChannelEndpointId(20L);
        entity.setInboundProtocol("openai");
        entity.setUpstreamProtocol("anthropic");
        entity.setDurationMs(1500L);
        entity.setSuccess(true);
        entity.setInputTokens(120);
        entity.setOutputTokens(45);
        entity.setErrorMessage(null);
        entity.setCalledAt(Instant.parse("2026-08-24T10:00:00Z"));
        return entity;
    }

    private CallLogDo createTestDo() {
        CallLogDo doEntity = new CallLogDo();
        doEntity.setId(1L);
        doEntity.setTraceId("trace-abc");
        doEntity.setUserId(1L);
        doEntity.setModel("gpt-4o");
        doEntity.setChannelId(10L);
        doEntity.setChannelEndpointId(20L);
        doEntity.setInboundProtocol("openai");
        doEntity.setUpstreamProtocol("anthropic");
        doEntity.setDurationMs(1500L);
        doEntity.setSuccess(true);
        doEntity.setInputTokens(120);
        doEntity.setOutputTokens(45);
        doEntity.setErrorMessage(null);
        doEntity.setCalledAt(Instant.parse("2026-08-24T10:00:00Z"));
        return doEntity;
    }

    private void assertEntityFields(CallLog result) {
        assertThat(result.getTraceId()).isEqualTo("trace-abc");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getModel()).isEqualTo("gpt-4o");
        assertThat(result.getChannelId()).isEqualTo(10L);
        assertThat(result.getChannelEndpointId()).isEqualTo(20L);
        assertThat(result.getInboundProtocol()).isEqualTo("openai");
        assertThat(result.getUpstreamProtocol()).isEqualTo("anthropic");
        assertThat(result.getDurationMs()).isEqualTo(1500L);
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getInputTokens()).isEqualTo(120);
        assertThat(result.getOutputTokens()).isEqualTo(45);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getCalledAt()).isEqualTo(Instant.parse("2026-08-24T10:00:00Z"));
    }

    @Nested
    @DisplayName("统计查询方法测试")
    class StatsTests {

        @Test
        @DisplayName("countSince 统计指定时间之后的调用次数（含边界）")
        void countSince_countsAfterTime() {
            // given
            CallLogDo before = new CallLogDo();
            before.setCalledAt(Instant.parse("2026-08-20T00:00:00Z"));
            CallLogDo at = new CallLogDo();
            at.setCalledAt(Instant.parse("2026-08-21T00:00:00Z"));
            CallLogDo after = new CallLogDo();
            after.setCalledAt(Instant.parse("2026-08-22T00:00:00Z"));
            when(repository.findAll()).thenReturn(List.of(before, at, after));

            // when
            long count = gateway.countSince(Instant.parse("2026-08-21T00:00:00Z"));

            // then：边界值（at）计入
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("sumTokensSince 累加指定时间后的 Token（输入+输出，null 按 0）")
        void sumTokensSince_sumsInputAndOutput() {
            // given
            CallLogDo a = new CallLogDo();
            a.setCalledAt(Instant.parse("2026-08-21T10:00:00Z"));
            a.setInputTokens(100);
            a.setOutputTokens(50);
            CallLogDo b = new CallLogDo();
            b.setCalledAt(Instant.parse("2026-08-21T11:00:00Z"));
            b.setInputTokens(10);
            b.setOutputTokens(null);
            when(repository.findAll()).thenReturn(List.of(a, b));

            // when
            long sum = gateway.sumTokensSince(Instant.parse("2026-08-21T00:00:00Z"));

            // then：100+50 + 10+0 = 160
            assertThat(sum).isEqualTo(160);
        }

        @Test
        @DisplayName("findDailyUsage 按天聚合请求数与 Token，忽略范围外记录")
        void findDailyUsage_groupsByDate() {
            // given：两天数据 + 1 条范围外（不硬编码日期字符串，避免测试机时区差异）
            CallLogDo day1a = new CallLogDo();
            day1a.setCalledAt(Instant.parse("2026-08-21T10:00:00Z"));
            day1a.setInputTokens(100);
            day1a.setOutputTokens(20);
            CallLogDo day1b = new CallLogDo();
            day1b.setCalledAt(Instant.parse("2026-08-21T11:00:00Z"));
            day1b.setInputTokens(50);
            CallLogDo day2 = new CallLogDo();
            day2.setCalledAt(Instant.parse("2026-08-22T10:00:00Z"));
            day2.setInputTokens(10);
            CallLogDo outOfRange = new CallLogDo();
            outOfRange.setCalledAt(Instant.parse("2026-08-23T10:00:00Z"));
            when(repository.findAll()).thenReturn(List.of(day1a, day1b, day2, outOfRange));

            // when
            List<DailyUsage> usage = gateway.findDailyUsage(
                    Instant.parse("2026-08-21T00:00:00Z"),
                    Instant.parse("2026-08-22T23:59:59Z"));

            // then：两天各一组
            assertThat(usage).hasSize(2);
            DailyUsage busy = usage.stream().filter(u -> u.requestCount() == 2).findFirst().orElseThrow();
            assertThat(busy.tokenCount()).isEqualTo(170);
            DailyUsage light = usage.stream().filter(u -> u.requestCount() == 1).findFirst().orElseThrow();
            assertThat(light.tokenCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("findModelUsage 按请求数降序取 Top N，忽略 model 为空的记录")
        void findModelUsage_returnsTopByCount() {
            // given
            CallLogDo gpt = new CallLogDo();
            gpt.setModel("gpt-4o");
            CallLogDo gpt2 = new CallLogDo();
            gpt2.setModel("gpt-4o");
            CallLogDo claude = new CallLogDo();
            claude.setModel("claude");
            CallLogDo noModel = new CallLogDo();
            noModel.setModel(null);
            when(repository.findAll()).thenReturn(List.of(gpt, gpt2, claude, noModel));

            // when
            List<ModelUsage> usage = gateway.findModelUsage(2);

            // then
            assertThat(usage).hasSize(2);
            assertThat(usage.get(0).model()).isEqualTo("gpt-4o");
            assertThat(usage.get(0).requestCount()).isEqualTo(2);
            assertThat(usage.get(1).model()).isEqualTo("claude");
            assertThat(usage.get(1).requestCount()).isEqualTo(1);
        }
    }
}
