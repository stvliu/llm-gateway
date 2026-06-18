package com.codingas.gateway.domain.application.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 应用-渠道授权关联实体
 *
 * <p>应用-渠道授权关联：决定应用可见的渠道集合。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑；
 * 渠道可见性判定由上层应用服务基于该关联集合完成。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>applicationId — 应用 ID，外键关联 applications.id</li>
 *   <li>channelId — 渠道 ID，外键关联 channels.id</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 *
 * <p>唯一约束：(application_id, channel_id) 组合唯一，见 V51 迁移 uk_app_channel。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ApplicationChannel extends BaseEntity {

    /** 应用 ID */
    private Long applicationId;

    /** 渠道 ID */
    private Long channelId;

    /**
     * 业务构造器（仅业务字段）
     *
     * <p>仅初始化业务字段，主键 id 与审计字段（createdBy/createdAt/updatedBy/updatedAt）
     * 继承自 {@link BaseEntity}，由基础设施层在持久化时填充。</p>
     *
     * @param applicationId 应用 ID
     * @param channelId     渠道 ID
     */
    public ApplicationChannel(Long applicationId, Long channelId) {
        this.applicationId = applicationId;
        this.channelId = channelId;
    }
}
