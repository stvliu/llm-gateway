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
package com.codingas.gateway.autoconfigure.iam;

import com.codingas.gateway.iam.IamConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * IAM 域自动装配（纯装配，零业务逻辑）
 *
 * <p>通过 {@code gateway.iam.enabled}（默认开启）控制域装配开关。</p>
 */
@AutoConfiguration
@Import(IamConfiguration.class)
@ConditionalOnProperty(prefix = "gateway.iam", name = "enabled", matchIfMissing = true)
public class IamAutoConfiguration {
}
