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
package com.codingas.gateway.provider.catalog.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * models.dev 模型目录同步配置属性
 */
@Data
@ConfigurationProperties(prefix = "gateway.catalog.sync")
public class CatalogSyncProperties {

    /** models.dev 模型数据源 URL */
    private String url = "https://models.dev/models.json";

    /** 拉取超时 */
    private Duration timeout = Duration.ofSeconds(30);

    /** 是否启用数据源（false 时 fetch 抛异常） */
    private boolean enabled = true;
}
