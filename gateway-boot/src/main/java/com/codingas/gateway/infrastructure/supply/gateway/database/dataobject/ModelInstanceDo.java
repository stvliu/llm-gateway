/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 模型实例数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_instances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mi_channel_model", columnNames = {"channel_id", "model_id"})
})
public class ModelInstanceDo extends BaseDo {

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "upstream_model_name", length = 256)
    private String upstreamModelName;

    /** 能力覆盖配置（JSONB） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities_override", columnDefinition = "jsonb")
    private Map<String, Boolean> capabilitiesOverride;

    /** 上下文窗口覆盖 */
    @Column(name = "context_window_override")
    private Integer contextWindowOverride;

    /** 优先级 */
    @Column(name = "priority")
    private Integer priority;

    /** 权重 */
    @Column(name = "weight")
    private Integer weight;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}