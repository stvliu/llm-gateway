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
package com.codingas.gateway.securitydata.threat;

import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * IP 黑名单 DO
 *
 * <p>JPA 实体，对应数据库 ip_blocklist 表。</p>
 */
@Entity
@Table(name = "ip_blocklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpBlocklistDo extends BaseDo {

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "blocked_by")
    private Long blockedBy;
}
