/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.repository;

import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA 操作日志实体
 */
@Setter
@Getter
@Entity
@Table(name = "channel_operation_logs")
public class ChannelOperationLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "channel_name", length = 200)
    private String channelName;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    @Column(name = "change_detail", columnDefinition = "TEXT")
    private String changeDetail;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "operator_ip", length = 50)
    private String operatorIp;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "batch_id")
    private Long batchId;

    public ChannelOperationLog toDomain() {
        ChannelOperationLog log = new ChannelOperationLog();
        log.setId(this.id);
        log.setChannelId(this.channelId);
        log.setChannelName(this.channelName);
        log.setAction(this.action);
        log.setActionLabel(this.actionLabel);
        log.setChangeDetail(this.changeDetail);
        log.setOperatorId(this.operatorId);
        log.setOperatorName(this.operatorName);
        log.setOperatorIp(this.operatorIp);
        log.setTraceId(this.traceId);
        log.setOperatedAt(this.operatedAt);
        log.setBatchId(this.batchId);
        return log;
    }

    public static ChannelOperationLogJpaEntity fromDomain(ChannelOperationLog log) {
        ChannelOperationLogJpaEntity entity = new ChannelOperationLogJpaEntity();
        entity.setId(log.getId());
        entity.setChannelId(log.getChannelId());
        entity.setChannelName(log.getChannelName());
        entity.setAction(log.getAction());
        entity.setActionLabel(log.getActionLabel());
        entity.setChangeDetail(log.getChangeDetail());
        entity.setOperatorId(log.getOperatorId());
        entity.setOperatorName(log.getOperatorName());
        entity.setOperatorIp(log.getOperatorIp());
        entity.setTraceId(log.getTraceId());
        entity.setOperatedAt(log.getOperatedAt());
        entity.setBatchId(log.getBatchId());
        return entity;
    }
}
