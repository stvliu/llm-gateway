/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.entity;

import java.time.LocalDateTime;

/**
 * 渠道操作日志 —— 记录所有对渠道的变更操作
 *
 * <p>每个操作记录包含操作人、操作时间、变更详情（JSON 格式），
 * 支持按渠道和操作人追溯审计。</p>
 */
public class ChannelOperationLog {

    /** 主键 ID */
    private Long id;

    /** 关联渠道 ID */
    private Long channelId;

    /** 操作时的渠道名称（快照，渠道删除后仍可读） */
    private String channelName;

    /** 操作类型 */
    private String action;

    /** 操作中文描述 */
    private String actionLabel;

    /** 变更详情（JSON，记录变更前后的字段值） */
    private String changeDetail;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作来源 IP */
    private String operatorIp;

    /** 全链路追踪 ID */
    private String traceId;

    /** 操作时间 */
    private LocalDateTime operatedAt;

    /** 批量操作 ID（同一批批量操作共享此 ID） */
    private Long batchId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getChangeDetail() {
        return changeDetail;
    }

    public void setChangeDetail(String changeDetail) {
        this.changeDetail = changeDetail;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorIp() {
        return operatorIp;
    }

    public void setOperatorIp(String operatorIp) {
        this.operatorIp = operatorIp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }
}
