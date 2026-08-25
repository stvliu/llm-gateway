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
 * <p>mock CallLogJpaRepository，覆盖 save/findByTraceId/findByUserId 及实体↔数据对象双向转换全字段断言。</p>
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

    @Nested
    @DisplayName("findByTraceId 方法测试")
    class FindByTraceIdTests {

        @Test
        @DisplayName("按 traceId 找到日志返回实体")
        void findByTraceId_existing_returnsEntity() {
            // given
            CallLogDo doEntity = createTestDo();
            when(repository.findByTraceId("trace-abc")).thenReturn(Optional.of(doEntity));

            // when
            Optional<CallLog> result = gateway.findByTraceId("trace-abc");

            // then
            assertThat(result).isPresent();
            assertEntityFields(result.get());
        }

        @Test
        @DisplayName("按 traceId 未找到返回空 Optional")
        void findByTraceId_missing_returnsEmpty() {
            // given
            when(repository.findByTraceId("nope")).thenReturn(Optional.empty());

            // when
            Optional<CallLog> result = gateway.findByTraceId("nope");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("按用户 ID 找到多条日志")
        void findByUserId_existing_returnsList() {
            // given
            when(repository.findByUserId(1L)).thenReturn(List.of(createTestDo(), createTestDo()));

            // when
            List<CallLog> result = gateway.findByUserId(1L);

            // then
            assertThat(result).hasSize(2);
            assertEntityFields(result.get(0));
            assertEntityFields(result.get(1));
        }

        @Test
        @DisplayName("用户无日志返回空列表")
        void findByUserId_noLogs_returnsEmpty() {
            // given
            when(repository.findByUserId(999L)).thenReturn(List.of());

            // when
            List<CallLog> result = gateway.findByUserId(999L);

            // then
            assertThat(result).isEmpty();
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
}
