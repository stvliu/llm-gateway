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
package com.codingas.gateway.boot.init;

import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON 资源文件读取器
 *
 * <p>封装 classpath 下 JSON 文件的定位与反序列化逻辑，
 * 复用 {@link JsonUtils} 统一管理 ObjectMapper，避免各 Loader 重复持有序列化基础设施。</p>
 */
@Slf4j
final class JsonResourceReader {

    private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver();

    private JsonResourceReader() {
        // 工具类，禁止实例化
    }

    /**
     * 读取单个 JSON 文件并反序列化为 List
     *
     * @param location classpath 路径（如 {@code data/sample/users.json}）
     * @param typeRef  目标类型引用
     * @param <T>      列表元素类型
     * @return 反序列化后的列表；文件不存在或读取失败时返回空列表
     */
    static <T> List<T> readList(String location, TypeReference<List<T>> typeRef) {
        Resource resource = RESOURCE_RESOLVER.getResource("classpath:" + location);
        if (!resource.exists()) {
            log.warn("JSON 资源文件不存在: {}", location);
            return Collections.emptyList();
        }
        try {
            List<T> result = JsonUtils.fromJson(resource.getInputStream(), typeRef);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("读取 JSON 资源文件失败: {}", location, e);
            return Collections.emptyList();
        }
    }

    /**
     * 读取匹配 glob 模式的多个 JSON 文件，合并结果
     *
     * @param pattern classpath glob 模式（如 {@code data/builtin/vendors/*.json}）
     * @param typeRef 目标类型引用
     * @param <T>     列表元素类型
     * @return 所有文件合并后的列表；无匹配文件或读取失败时返回空列表
     */
    static <T> List<T> readListFromPattern(String pattern, TypeReference<List<T>> typeRef) {
        try {
            Resource[] resources = RESOURCE_RESOLVER.getResources("classpath:" + pattern);
            List<T> result = new ArrayList<>();
            for (Resource resource : resources) {
                log.info("  解析文件: {}", resource.getFilename());
                List<T> items = JsonUtils.fromJson(resource.getInputStream(), typeRef);
                if (items != null) {
                    result.addAll(items);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("读取 JSON 资源文件失败: {}", pattern, e);
            return Collections.emptyList();
        }
    }
}
