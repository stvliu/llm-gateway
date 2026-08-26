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
package com.codingas.gateway.proxy.routing;

/**
 * 路由策略枚举
 *
 * <p>定义模型请求的路由策略。</p>
 */
public enum RoutingStrategy {
    /** 随机路由 */
    RANDOM,

    /** 加权路由 */
    WEIGHTED,

    /** 故障转移路由 */
    FAILOVER,

    /** 成本优化路由 */
    COST_OPTIMIZED,

    /** 延迟优化路由 */
    LATENCY_OPTIMIZED
}