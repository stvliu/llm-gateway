/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
