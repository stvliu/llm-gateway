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
