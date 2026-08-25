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
package com.codingas.gateway.provider.vendor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 提供商下嵌套模型用例入参
 *
 * @param modelName     模型名称
 * @param displayName   展示名称
 * @param contextWindow 上下文窗口
 * @param inputPrice    输入单价（预留）
 * @param outputPrice   输出单价（预留）
 * @param capabilities  能力覆盖配置
 */
public record ModelNestedCommand(
        String modelName,
        String displayName,
        Integer contextWindow,
        BigDecimal inputPrice,
        BigDecimal outputPrice,
        Map<String, Boolean> capabilities
) {
}
