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
package com.codingas.gateway.autoconfigure.usage;

import com.codingas.gateway.usage.UsageConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Usage 域自动装配（纯装配，零业务逻辑）
 *
 * <p>通过 {@code gateway.usage.enabled}（默认开启）控制域装配开关。</p>
 */
@AutoConfiguration
@Import(UsageConfiguration.class)
@ConditionalOnProperty(prefix = "gateway.usage", name = "enabled", matchIfMissing = true)
public class UsageAutoConfiguration {
}
