/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JSON 工具类
 *
 * <p>统一的 JSON 序列化/反序列化工具，支持两种模式：</p>
 * <ul>
 *   <li>Spring 环境：自动使用容器中已配置的 ObjectMapper（包含日期格式、时区等配置）</li>
 *   <li>非 Spring 环境：使用默认配置的 ObjectMapper</li>
 * </ul>
 *
 * <p>默认配置对齐 application.yml：</p>
 * <ul>
 *   <li>禁用 WRITE_DATES_AS_TIMESTAMPS</li>
 *   <li>禁用 FAIL_ON_UNKNOWN_PROPERTIES</li>
 *   <li>禁用 FAIL_ON_EMPTY_BEANS</li>
 * </ul>
 */
@Slf4j
public final class JsonUtils {

    /**
     * ObjectMapper 实例持有者
     * Spring 环境下由 JsonUtilsInitializer 注入，非 Spring 环境延迟初始化默认实例
     */
    private static final AtomicReference<ObjectMapper> OBJECT_MAPPER_HOLDER = new AtomicReference<>();

    private JsonUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 设置 ObjectMapper 实例（供 Spring 容器初始化时调用）
     *
     * @param objectMapper Spring 容器中配置的 ObjectMapper
     */
    public static void setObjectMapper(ObjectMapper objectMapper) {
        OBJECT_MAPPER_HOLDER.set(objectMapper);
        log.debug("JsonUtils ObjectMapper 已设置: {}", objectMapper.getClass().getName());
    }

    /**
     * 获取当前 ObjectMapper 实例
     *
     * <p>优先返回 Spring 注入的实例，否则创建并缓存默认配置实例</p>
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = OBJECT_MAPPER_HOLDER.get();
        if (mapper == null) {
            synchronized (JsonUtils.class) {
                mapper = OBJECT_MAPPER_HOLDER.get();
                if (mapper == null) {
                    mapper = createDefaultObjectMapper();
                    OBJECT_MAPPER_HOLDER.set(mapper);
                    log.debug("JsonUtils 使用默认 ObjectMapper");
                }
            }
        }
        return mapper;
    }

    /**
     * 重置 ObjectMapper（仅供测试使用）
     */
    public static void reset() {
        OBJECT_MAPPER_HOLDER.set(null);
    }

    /**
     * 创建默认配置的 ObjectMapper
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 对齐 application.yml 配置
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    // ==================== 序列化方法 ====================

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串，序列化失败返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化对象失败: {}", obj.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将对象序列化为 JSON 字符串（美化格式）
     *
     * @param obj 要序列化的对象
     * @return 格式化的 JSON 字符串，序列化失败返回 null
     */
    public static String toJsonPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化对象失败: {}", obj.getClass().getName(), e);
            return null;
        }
    }

    // ==================== 反序列化方法 ====================

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("反序列化失败, 目标类型: {}, JSON: {}", clazz.getName(), truncate(json, 200), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象（支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 目标类型引用
     * @param <T>           泛型类型
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.warn("反序列化失败, 目标类型: {}, JSON: {}", typeReference.getType(), truncate(json, 200), e);
            return null;
        }
    }

    /**
     * 从输入流反序列化为指定类型的对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(inputStream, clazz);
        } catch (IOException e) {
            log.warn("从输入流反序列化失败, 目标类型: {}", clazz.getName(), e);
            return null;
        }
    }

    /**
     * 从输入流反序列化为指定类型的对象（支持泛型）
     *
     * @param inputStream   输入流
     * @param typeReference 目标类型引用
     * @param <T>           泛型类型
     * @return 反序列化后的对象，失败返回 null
     */
    public static <T> T fromJson(InputStream inputStream, TypeReference<T> typeReference) {
        if (inputStream == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(inputStream, typeReference);
        } catch (IOException e) {
            log.warn("从输入流反序列化失败, 目标类型: {}", typeReference.getType(), e);
            return null;
        }
    }

    // ==================== JsonNode 方法 ====================

    /**
     * 将 JSON 字符串解析为 JsonNode 树
     *
     * @param json JSON 字符串
     * @return JsonNode，失败返回 null
     */
    public static JsonNode readTree(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return getObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("解析 JSON 树失败: {}", truncate(json, 200), e);
            return null;
        }
    }

    // ==================== Map 转换方法 ====================

    /**
     * 将 JSON 字符串解析为 Map
     *
     * @param json JSON 字符串
     * @return Map 对象，失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return getObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON 转 Map 失败: {}", truncate(json, 200), e);
            return new HashMap<>();
        }
    }

    /**
     * 将对象转换为 Map
     *
     * @param obj 对象
     * @return Map 对象，失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        try {
            String json = getObjectMapper().writeValueAsString(obj);
            return getObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("对象转 Map 失败: {}", obj.getClass().getName(), e);
            return new HashMap<>();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 截断字符串用于日志输出
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
