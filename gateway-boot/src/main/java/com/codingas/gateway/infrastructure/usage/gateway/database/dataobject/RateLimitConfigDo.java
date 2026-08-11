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
package com.codingas.gateway.infrastructure.usage.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 限流配置 DO
 *
 * <p>JPA 实体，对应数据库 rate_limit_configs 表。</p>
 */
@Entity
@Table(name = "rate_limit_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfigDo extends BaseDo {

    @Column(name = "name")
    private String name;

    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Column(name = "bucket_size")
    private Integer bucketSize;

    @Column(name = "refill_rate")
    private Integer refillRate;

    @Column(name = "enabled")
    private Boolean enabled;
}
