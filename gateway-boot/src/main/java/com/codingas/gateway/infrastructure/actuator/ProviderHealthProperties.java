/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.actuator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Provider 健康检测配置属性
 */
@Data
@ConfigurationProperties(prefix = "gateway.health.provider")
public class ProviderHealthProperties {

    /** 超过此时间无请求则重新主动探测 */
    private Duration staleThreshold = Duration.ofSeconds(300);

    /** 连续失败 N 次标记 DOWN */
    private int failureThreshold = 3;

    /** 连续成功 N 次恢复 UP */
    private int successThreshold = 2;

    /** 主动探测超时 */
    private Duration probeTimeout = Duration.ofSeconds(10);
}
