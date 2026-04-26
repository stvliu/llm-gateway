package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 审计日志实体
 *
 * <p>记录用户的关键操作行为。</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "resource")
    private String resource;

    @Column(name = "result")
    private String result;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}