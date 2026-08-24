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
package com.codingas.gateway.boot.init;

import com.codingas.gateway.boot.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 数据初始化编排器
 *
 * <p>收集所有 {@link DataLoader} Bean，按阶段排序后依次驱动。
 * 每个 Loader 通过 {@link DataLoadContext} 传递阶段间依赖数据。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final List<DataLoader> loaders;
    private final GatewayProperties gatewayProperties;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始初始化网关数据...");

        DataLoadContext context = new DataLoadContext();

        loaders.stream()
                .sorted(Comparator.comparingInt(loader -> loader.getPhase().getOrder()))
                .filter(loader -> loader.isEnabled(gatewayProperties))
                .forEach(loader -> {
                    log.info("执行阶段: {} ({})", loader.getPhase(), loader.getClass().getSimpleName());
                    loader.load(context);
                });

        log.info("网关数据初始化完成");
    }
}
