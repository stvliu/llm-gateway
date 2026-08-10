/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.threat.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
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
