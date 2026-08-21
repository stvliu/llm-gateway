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
package com.codingas.gateway.application.init;

import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.provider.channel.Channel;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据初始化阶段间上下文
 *
 * <p>类型安全的键值容器，Loader 通过 {@link #get(Class)} 读取上游数据、
 * {@link #set(Class, Object)} 写入下游数据。</p>
 *
 * <p>内置三种索引类型用于传递实体映射：</p>
 * <ul>
 *   <li>{@link ChannelIndex} — 渠道 key → Channel</li>
 *   <li>{@link ApplicationIndex} — 应用编码 → Application</li>
 *   <li>{@link UserIndex} — 用户名 → User</li>
 * </ul>
 */
public class DataLoadContext {

    private final Map<Class<?>, Object> store = new HashMap<>();

    /**
     * 存入数据
     */
    public <T> void set(Class<T> type, T value) {
        store.put(type, value);
    }

    /**
     * 取出数据
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) store.get(type));
    }

    /**
     * 取出数据（非空，取不到抛异常）
     */
    @SuppressWarnings("unchecked")
    public <T> T getRequired(Class<T> type) {
        T value = (T) store.get(type);
        if (value == null) {
            throw new IllegalStateException("上下文中缺少 " + type.getSimpleName()
                    + "，请确认前置阶段已执行");
        }
        return value;
    }

    // ========== 内置索引类型 ==========

    /** 渠道 key → Channel 映射 */
    @Getter
    public static final class ChannelIndex {
        private final Map<String, Channel> map;
        public ChannelIndex(Map<String, Channel> map) { this.map = Map.copyOf(map); }
    }

    /** 应用编码 → Application 映射 */
    @Getter
    public static final class ApplicationIndex {
        private final Map<String, Application> map;
        public ApplicationIndex(Map<String, Application> map) { this.map = Map.copyOf(map); }
    }

    /** 用户名 → User 映射 */
    @Getter
    public static final class UserIndex {
        private final Map<String, User> map;
        public UserIndex(Map<String, User> map) { this.map = Map.copyOf(map); }
    }
}
