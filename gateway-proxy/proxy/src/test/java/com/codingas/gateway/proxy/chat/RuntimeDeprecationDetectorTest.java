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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RuntimeDeprecationDetector 运行期废弃检测单元测试
 *
 * <p>验证数据面兜底确认逻辑：开关校验 → 内存计数 → 达到 confirm-count 调
 * {@link ModelDeprecationService#markDeprecated} 并清计数。</p>
 * <ul>
 *   <li>总开关关闭：不计数不确认</li>
 *   <li>运行期子开关关闭：不确认</li>
 *   <li>达到确认次数触发标记并清计数（再次计数不立即触发）</li>
 *   <li>不同模型独立计数</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuntimeDeprecationDetector 运行期废弃检测")
class RuntimeDeprecationDetectorTest {

    @Mock
    private SystemSettingService settingService;
    @Mock
    private ModelDeprecationService deprecationService;
    @InjectMocks
    private RuntimeDeprecationDetector detector;

    @Test
    @DisplayName("总开关关闭时不计数不确认")
    void disabled_totalSwitch_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(false);
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("运行期子开关关闭时不确认")
    void disabled_runtimeSwitch_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(false);
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("达到确认次数触发标记并清计数")
    void reachesThreshold_confirmsAndResets() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(true);
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(3);

        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());

        detector.onModelNotFound("gpt-4");
        verify(deprecationService).markDeprecated(eq("gpt-4"), contains("model_not_found"));

        // 清计数后再次计数，不应立即触发
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, times(1)).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("不同模型独立计数")
    void countsPerModelIndependently() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(true);
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(3);

        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("claude-3");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }
}
