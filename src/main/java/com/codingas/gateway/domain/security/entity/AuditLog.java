package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 审计日志实体
 *
 * <p>记录用户的关键操作行为。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class AuditLog extends BaseEntity {

    private Long userId;

    private String action;

    private String resource;

    private String result;

    private String ipAddress;
}
