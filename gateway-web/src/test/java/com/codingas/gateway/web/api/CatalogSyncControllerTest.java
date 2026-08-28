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

import com.codingas.gateway.provider.catalog.sync.CatalogSyncLog;
import com.codingas.gateway.provider.catalog.sync.CatalogSyncLogRepository;
import com.codingas.gateway.provider.catalog.sync.CatalogSyncReport;
import com.codingas.gateway.provider.catalog.sync.ModelCatalogSyncService;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CatalogSyncController 端点契约测试
 *
 * <p>使用 standalone setup + Mock 各依赖，验证模型目录同步触发与状态查询
 * 端点的路由、HTTP 状态码与响应契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogSyncController 端点契约")
class CatalogSyncControllerTest {

    @Mock
    private ModelCatalogSyncService syncService;

    @Mock
    private CatalogSyncLogRepository logRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CatalogSyncController controller = new CatalogSyncController(syncService, logRepository);
        // 装配 GlobalExceptionHandler 以便异常被转为统一响应（与生产 Web 上下文一致）；
        // Jackson 关闭时间戳输出（与 Spring Boot 自动配置一致，Instant 序列化为 ISO-8601）
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/catalog/sync 触发同步并返回报告")
    void sync_returnsReport() throws Exception {
        // given
        CatalogSyncReport report = CatalogSyncReport.builder()
                .success(true).addedCount(100).updatedCount(20)
                .skippedCount(0).failedCount(0)
                .messages(List.of("同步完成")).syncedAt(Instant.parse("2026-08-28T00:00:00Z"))
                .build();
        when(syncService.sync()).thenReturn(report);

        // when & then
        mockMvc.perform(post("/api/v1/catalog/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.addedCount").value(100))
                .andExpect(jsonPath("$.updatedCount").value(20))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.messages[0]").value("同步完成"))
                .andExpect(jsonPath("$.syncedAt").value("2026-08-28T00:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/sync/status 返回最近同步记录")
    void status_returnsLatestLog() throws Exception {
        // given
        CatalogSyncLog log = new CatalogSyncLog();
        log.setResult("SUCCESS");
        log.setAddedCount(100);
        log.setUpdatedCount(20);
        log.setSkippedCount(0);
        log.setFailedCount(0);
        log.setMessage("同步完成");
        log.setSyncedAt(Instant.parse("2026-08-28T00:00:00Z"));
        when(logRepository.findLatest()).thenReturn(Optional.of(log));

        // when & then
        mockMvc.perform(get("/api/v1/catalog/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.addedCount").value(100))
                .andExpect(jsonPath("$.updatedCount").value(20))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.message").value("同步完成"))
                .andExpect(jsonPath("$.syncedAt").value("2026-08-28T00:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/sync/status 无记录时返回 204")
    void status_noLog_returnsNoContent() throws Exception {
        // given
        when(logRepository.findLatest()).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/v1/catalog/sync/status"))
                .andExpect(status().isNoContent());
    }
}
