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
package com.codingas.simulator.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 覆盖配置，根据请求的 Authorization Header 返回不同响应。
 * <p>
 * 匹配规则：检查 API Key 是否以 keyPrefix 开头。
 * 用于 Key 故障转移验证场景。
 */
public class ApiKeyOverrideConfig {

    private final Map<String, SimulatorModeService.SimulatorMode> overrides = new ConcurrentHashMap<>();

    /**
     * 设置 API Key 前缀覆盖。
     *
     * @param keyPrefix API Key 前缀
     * @param mode      覆盖模式
     */
    public void setOverride(String keyPrefix, SimulatorModeService.SimulatorMode mode) {
        overrides.put(keyPrefix, mode);
    }

    /**
     * 移除指定前缀的覆盖。
     *
     * @param keyPrefix API Key 前缀
     */
    public void removeOverride(String keyPrefix) {
        overrides.remove(keyPrefix);
    }

    /**
     * 清除所有覆盖规则。
     */
    public void clearAll() {
        overrides.clear();
    }

    /**
     * 获取所有覆盖规则。
     *
     * @return 不可修改的覆盖规则 Map
     */
    public Map<String, SimulatorModeService.SimulatorMode> getOverrides() {
        return Map.copyOf(overrides);
    }

    /**
     * 匹配给定 API Key 的覆盖模式。
     * <p>
     * 遍历所有前缀规则，返回第一个匹配的覆盖模式。
     *
     * @param apiKey API Key
     * @return 匹配的覆盖模式，无匹配时返回 empty
     */
    public Optional<SimulatorModeService.SimulatorMode> matchOverride(String apiKey) {
        if (apiKey == null) return Optional.empty();
        for (Map.Entry<String, SimulatorModeService.SimulatorMode> entry : overrides.entrySet()) {
            if (apiKey.startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
