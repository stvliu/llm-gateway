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
package com.codingas.gateway.stats;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Stats 域装配入口
 *
 * <p>限定扫描本域核心包 {@code com.codingas.gateway.stats}（无 data 绑定包），
 * 并扫描本域 @ConfigurationProperties。</p>
 */
@Configuration
@ComponentScan(basePackages = {
        "com.codingas.gateway.stats"
})
@ConfigurationPropertiesScan
public class StatsConfiguration {
}
