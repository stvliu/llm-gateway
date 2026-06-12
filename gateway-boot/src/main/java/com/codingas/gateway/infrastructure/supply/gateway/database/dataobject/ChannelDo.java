package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "channels")
public class ChannelDo extends BaseDo {

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", length = 32)
    private com.codingas.gateway.domain.supply.enums.BillingMode billingMode;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "timeout")
    private Integer timeout;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}