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
