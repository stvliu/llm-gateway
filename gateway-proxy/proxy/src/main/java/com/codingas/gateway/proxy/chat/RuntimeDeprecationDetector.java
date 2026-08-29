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
package com.codingas.gateway.proxy.chat;

import com.codingas.gateway.provider.model.ModelDeprecationService;
import com.codingas.gateway.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行期废弃检测器（数据面兜底确认）
 *
 * <p>数据面识别到 model_not_found 时由 {@link #onModelNotFound} 计数，
 * 达到 {@code catalog.deprecation.confirm-count} 后确认模型废弃（幂等）。
 * 计数使用进程内内存（{@link ConcurrentHashMap}），开发/本地/单实例不依赖 Redis；
 * 多实例各自计数，确认动作幂等无副作用。开关关闭时完全不干预（正常路径零开销）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuntimeDeprecationDetector {

    /** 连续确认次数：modelName → 计数 */
    private final ConcurrentHashMap<String, Integer> confirmations = new ConcurrentHashMap<>();

    private final SystemSettingService settingService;
    private final ModelDeprecationService deprecationService;

    /**
     * 记录一次 model_not_found 信号；达到阈值确认废弃并清计数
     *
     * @param modelName 模型名
     */
    public void onModelNotFound(String modelName) {
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.runtime.enabled", true)) {
            log.debug("模型废弃自动化已关闭，忽略 model_not_found: {}", modelName);
            return;
        }
        // 确认次数下限为 1：配置非法（≤0）时避免永远无法达到阈值导致确认通道失效
        int confirmCount = Math.max(1, settingService.getInt("catalog.deprecation.confirm-count", 3));
        int count = confirmations.merge(modelName, 1, Integer::sum);
        if (count >= confirmCount) {
            confirmations.remove(modelName);
            log.info("模型连续 {} 次确认废弃, modelName={}", count, modelName);
            try {
                deprecationService.markDeprecated(modelName,
                        "上游确认模型已废弃（model_not_found）");
            } catch (RuntimeException e) {
                // 确认失败不阻断请求（错误已按原样返回用户）
                log.error("自动标记废弃失败, modelName={}: {}", modelName, e.getMessage());
            }
        }
    }
}
