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

import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.proxy.experience.ModelExperienceService;
import com.codingas.gateway.web.advice.IamExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExperienceController 单元测试
 *
 * <p>覆盖体验中心授权校验失败（IAE）转 403 的转换链路：
 * 服务抛 {@link IllegalArgumentException} → Controller 捕获转 {@link ForbiddenException}
 * → {@link IamExceptionHandler} 映射为 403。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExperienceController 测试")
class ExperienceControllerTest {

    @Mock
    private ModelExperienceService modelExperienceService;

    @InjectMocks
    private ExperienceController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone 装配：授权异常由 IamExceptionHandler 映射为 403
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IamExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("chatStream：授权校验失败（IAE）转 403")
    void chatStream_authorizationFailure_returnsForbidden() throws Exception {
        // given：体验中心授权校验失败（渠道归属/角色门控）由服务抛 IllegalArgumentException
        when(modelExperienceService.chatStream(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("渠道不属于当前应用"));

        // when/then：Controller 捕获 IAE 转 ForbiddenException，由 IamExceptionHandler 映射为 403
        mockMvc.perform(post("/api/v1/experience/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gpt-4\",\"protocolName\":\"openai\"}")
                        .requestAttr("identity", Identity.of(1L, "USER", 10L, 100L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
