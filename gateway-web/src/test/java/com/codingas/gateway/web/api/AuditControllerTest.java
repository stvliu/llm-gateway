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

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditService;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.web.advice.ApiResponseWrapperAdvice;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import com.codingas.gateway.web.api.dto.AuditLogQueryRequest;
import com.codingas.gateway.web.api.dto.AuditLogResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuditController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditController 测试")
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 装配 ApiResponseWrapperAdvice 模拟生产统一响应包装（DELETE 返回普通 Map 会被包装为 {data: {...}}）；
        // GlobalExceptionHandler 以便异常被转为统一响应；Jackson 关闭时间戳输出（Instant 序列化为 ISO-8601）
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiResponseWrapperAdvice(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("DELETE /api/v1/audit-logs?days=30 手动清理")
    void deleteAuditLogs_returnsDeletedCount() throws Exception {
        when(auditService.deleteBefore(any())).thenReturn(10);
        mockMvc.perform(delete("/api/v1/audit-logs").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(10));
        verify(auditService).deleteBefore(any());
    }

    @Test
    @DisplayName("query 透传筛选条件并返回分页响应 DTO")
    void query_passesFiltersAndMapsResponse() {
        // given
        AuditLogQueryRequest request = new AuditLogQueryRequest();
        request.setPage(1);
        request.setLimit(20);
        request.setUserId(3L);
        request.setAction("POST /api/v1/channels");
        request.setResult("SUCCESS");
        request.setStartTime(Instant.parse("2026-08-27T00:00:00Z"));
        request.setEndTime(Instant.parse("2026-08-27T23:59:59Z"));

        AuditLog log = new AuditLog();
        log.setId(9L);
        log.setUserId(3L);
        log.setAction("POST /api/v1/channels");
        log.setResource("/api/v1/channels");
        log.setResult("SUCCESS");
        log.setIpAddress("192.168.1.1");
        log.setCreatedAt(Instant.parse("2026-08-27T10:00:00Z"));

        when(auditService.query(any())).thenAnswer(invocation -> {
            var q = invocation.getArgument(0, com.codingas.gateway.audit.AuditLogQuery.class);
            assertThat(q.getUserId()).isEqualTo(3L);
            assertThat(q.getAction()).isEqualTo("POST /api/v1/channels");
            assertThat(q.getResult()).isEqualTo("SUCCESS");
            assertThat(q.getStartTime()).isEqualTo(Instant.parse("2026-08-27T00:00:00Z"));
            assertThat(q.getEndTime()).isEqualTo(Instant.parse("2026-08-27T23:59:59Z"));
            return PageResponse.of(List.of(log), q.getPage(), q.getLimit(), 1);
        });

        // when
        PageResponse<AuditLogResponse> page = controller.query(request);

        // then
        assertThat(page.getPagination().getTotal()).isEqualTo(1);
        assertThat(page.getPagination().getPage()).isEqualTo(1);
        assertThat(page.getItems()).singleElement().satisfies(resp -> {
            assertThat(resp.getId()).isEqualTo(9L);
            assertThat(resp.getUserId()).isEqualTo(3L);
            assertThat(resp.getAction()).isEqualTo("POST /api/v1/channels");
            assertThat(resp.getResult()).isEqualTo("SUCCESS");
            assertThat(resp.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(resp.getCreatedAt()).isEqualTo(Instant.parse("2026-08-27T10:00:00Z"));
        });
    }

    @Test
    @DisplayName("query 无筛选条件时透传默认分页")
    void query_defaultPaging_whenNoFilters() {
        // given
        AuditLogQueryRequest request = new AuditLogQueryRequest(); // page=1 limit=20

        when(auditService.query(any())).thenReturn(PageResponse.of(List.of(), 1, 20, 0));

        // when
        PageResponse<AuditLogResponse> page = controller.query(request);

        // then
        ArgumentCaptor<com.codingas.gateway.audit.AuditLogQuery> captor =
                ArgumentCaptor.forClass(com.codingas.gateway.audit.AuditLogQuery.class);
        org.mockito.Mockito.verify(auditService).query(captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(1);
        assertThat(captor.getValue().getLimit()).isEqualTo(20);
        assertThat(page.getItems()).isEmpty();
    }
}
