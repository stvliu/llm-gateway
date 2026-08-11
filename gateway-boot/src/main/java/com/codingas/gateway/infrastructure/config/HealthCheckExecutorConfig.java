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
package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 健康检查独立线程池配置
 *
 * <p>避免连通性测试阻塞 Tomcat 主线程池：</p>
 * <ul>
 *   <li>core 4 / max 16 / queue 50</li>
 *   <li>线程命名前缀 health-check-</li>
 *   <li>拒绝策略：CallerRunsPolicy（队列满时由调用线程执行，保证可观察性）</li>
 * </ul>
 */
@Configuration
@Slf4j
public class HealthCheckExecutorConfig {

    /**
     * 健康检查专用 Executor Bean
     */
    @Bean(name = "healthCheckExecutor")
    public Executor healthCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("health-check-");
        executor.setKeepAliveSeconds(60);
        // 队列满时由调用线程执行，避免任务静默丢弃
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("初始化 healthCheckExecutor: core=4, max=16, queue=50");
        return executor;
    }
}
