package com.codingas.gateway.infrastructure.application.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用-渠道授权关联数据对象
 *
 * <p>对应 application_channels 表；主键 id 与审计字段继承自 {@link BaseDo}。
 * (application_id, channel_id) 组合唯一约束由 V51 迁移 uk_app_channel 定义。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_channels")
public class ApplicationChannelDo extends BaseDo {

    /** 应用 ID，外键关联 applications.id */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** 渠道 ID，外键关联 channels.id */
    @Column(name = "channel_id", nullable = false)
    private Long channelId;
}
