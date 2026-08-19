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
package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道健康状态聚合枚举。
 *
 * <p>由连通性测试矩阵聚合得出，遵循 last-write-wins 写入策略。</p>
 *
 * <ul>
 *   <li>HEALTHY：全部 Key 通过且各自至少返回 1 个可用模型</li>
 *   <li>DEGRADED：部分通过、部分失败</li>
 *   <li>FAILED：全部 Key 失败或无任何可用模型</li>
 *   <li>UNKNOWN：无 Key 或未执行过测试</li>
 * </ul>
 */
public enum ChannelHealthStatus {
    HEALTHY,
    DEGRADED,
    FAILED,
    UNKNOWN
}
