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
package com.codingas.gateway.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JsonUtils 初始化器
 *
 * <p>在 Spring 容器启动时，将容器中已配置的 ObjectMapper 注入 JsonUtils。</p>
 * <p>确保 JsonUtils 在 Spring 环境下使用统一的配置（日期格式、时区等）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonUtilsInitializer {

    private final ObjectMapper objectMapper;

    /**
     * 将 Spring 容器中的 ObjectMapper 注入 JsonUtils
     */
    @PostConstruct
    public void init() {
        JsonUtils.setObjectMapper(objectMapper);
        log.info("JsonUtils 已注入 Spring 容器的 ObjectMapper");
    }
}
