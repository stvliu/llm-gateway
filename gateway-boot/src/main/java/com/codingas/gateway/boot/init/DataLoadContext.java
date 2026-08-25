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

import com.codingas.gateway.provider.channel.Channel;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据初始化阶段间上下文
 *
 * <p>类型安全的键值容器，Loader 通过 {@link #getRequired(Class)} 读取上游数据、
 * {@link #set(Class, Object)} 写入下游数据。</p>
 *
 * <p>内置索引类型用于传递实体映射：{@link ChannelIndex} — 渠道 key → Channel。</p>
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
}
