package com.codingas.gateway.domain.alert.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 告警模板实体
 *
 * <p>预定义的告警消息模板。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class AlertTemplate extends BaseEntity {

    private String templateCode;

    private String name;

    private String subject;

    private String body;

    private AlertChannel channel;

    private String language;

    public enum AlertChannel {
        EMAIL,
        SMS,
        WEBHOOK,
        SLACK,
        DINGTALK,
        FEISHU
    }
}
