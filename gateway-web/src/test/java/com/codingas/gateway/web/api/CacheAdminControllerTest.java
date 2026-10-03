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

import com.codingas.gateway.iam.application.ApplicationChannelConfigProvider;
import com.codingas.gateway.web.advice.ApiResponseWrapperAdvice;
import com.codingas.gateway.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CacheAdminController 单元测试
 *
 * <p>使用 standalone setup + Mock ApplicationChannelConfigProvider，验证缓存失效端点的
 * 路由与参数分支（按应用 evict / 全清 clearAll），不连数据库。</p>
 *
 * <p>授权（仅 ADMIN）由拦截器链保证：{@code /api/v1/admin/cache/evict} 未匹配
 * {@code AuthorizationService.CONTROL_RULES} 任何规则 → 默认拒绝分支仅 ADMIN 放行，
 * controller 单测不重复验证授权。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheAdminController 测试")
class CacheAdminControllerTest {

    @Mock
    private ApplicationChannelConfigProvider provider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CacheAdminController controller = new CacheAdminController(provider);
        // 装配 ApiResponseWrapperAdvice 模拟生产统一响应包装（void 返回包装为成功 ApiResponse）；
        // GlobalExceptionHandler 以便异常被转为统一响应（与生产一致）
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiResponseWrapperAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("evict 带 applicationId：仅失效指定应用")
    void evict_withApplicationId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cache/evict").param("applicationId", "100"))
                .andExpect(status().isOk());
        verify(provider).evict(100L);
        verify(provider, never()).clearAll();
    }

    @Test
    @DisplayName("evict 无 applicationId：清空全部缓存")
    void evict_withoutApplicationId_clearsAll() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cache/evict"))
                .andExpect(status().isOk());
        verify(provider, never()).evict(any());
        verify(provider).clearAll();
    }
}
