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
package com.codingas.gateway.usage;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Usage 域装配入口
 *
 * <p>限定扫描本域核心包 {@code com.codingas.gateway.usage} 与绑定包
 * {@code com.codingas.gateway.usagedata}（绑定模块过渡期由核心兼扫，
 * data-starter 留后续），并扫描本域 @ConfigurationProperties。</p>
 */
@Configuration
@ComponentScan(basePackages = {
        "com.codingas.gateway.usage",
        "com.codingas.gateway.usagedata"
})
@ConfigurationPropertiesScan
public class UsageConfiguration {
}
