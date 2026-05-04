package com.codingas.gateway.infrastructure.audit.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 审计日志 DO
 *
 * <p>JPA 实体，对应数据库 audit_logs 表。</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDo extends BaseDo {

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
}
