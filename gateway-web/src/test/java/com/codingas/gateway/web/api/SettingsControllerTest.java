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

import com.codingas.gateway.settings.SystemSetting;
import com.codingas.gateway.settings.SystemSettingService;
import com.codingas.gateway.web.advice.ApiResponseWrapperAdvice;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SettingsController 端点契约测试
 *
 * <p>使用 standalone setup + Mock SystemSettingService，验证系统设置查询与更新
 * 端点的路由、HTTP 状态码与响应契约，不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsController 端点契约")
class SettingsControllerTest {

    @Mock
    private SystemSettingService settingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SettingsController controller = new SettingsController(settingService);
        // 装配 ApiResponseWrapperAdvice 模拟生产统一响应包装（List/单对象被包装进 data）；
        // GlobalExceptionHandler 以便 IllegalArgumentException 等异常被转为统一响应（与生产一致）
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiResponseWrapperAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/settings 返回全部配置")
    void getAll_returnsSettings() throws Exception {
        // given
        SystemSetting s = new SystemSetting();
        s.setSettingKey("audit.retention.days");
        s.setSettingValue("90");
        s.setGroupName("AUDIT");
        s.setValueType("NUMBER");
        s.setEditable(true);
        when(settingService.getAll()).thenReturn(List.of(s));

        // when & then
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].settingKey").value("audit.retention.days"))
                .andExpect(jsonPath("$.data[0].settingValue").value("90"));
    }

    @Test
    @DisplayName("PUT /api/v1/settings/{key} 更新配置")
    void update_returnsUpdated() throws Exception {
        // given
        SystemSetting s = new SystemSetting();
        s.setSettingKey("audit.retention.days");
        s.setSettingValue("120");
        s.setGroupName("AUDIT");
        s.setValueType("NUMBER");
        s.setEditable(true);
        when(settingService.update("audit.retention.days", "120")).thenReturn(s);

        // when & then
        mockMvc.perform(put("/api/v1/settings/audit.retention.days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"120\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("120"));
    }

    @Test
    @DisplayName("PUT 非法参数返回 400")
    void update_invalidValue_badRequest() throws Exception {
        // given
        when(settingService.update(anyString(), anyString())).thenThrow(new IllegalArgumentException("配置项不存在"));

        // when & then
        mockMvc.perform(put("/api/v1/settings/nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"1\"}"))
                .andExpect(status().isBadRequest());
    }
}
