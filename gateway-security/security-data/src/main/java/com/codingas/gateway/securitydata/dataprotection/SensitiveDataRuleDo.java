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
package com.codingas.gateway.securitydata.dataprotection;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 敏感数据规则 DO
 *
 * <p>JPA 实体，对应数据库 sensitive_data_rules 表。</p>
 */
@Entity
@Table(name = "sensitive_data_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveDataRuleDo extends BaseDo {

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "regex_pattern", nullable = false)
    private String regexPattern;

    @Column(name = "mask_format")
    private String maskFormat;

    @Column(name = "enabled")
    private Boolean enabled;
}
