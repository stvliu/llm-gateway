package com.codingas.gateway.infrastructure.application.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用聚合根数据对象
 *
 * <p>对应 applications 表；主键与审计字段（created_by/created_at/updated_by/updated_at）
 * 继承自 {@link BaseDo}，由 AuditingEntityListener 自动填充。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "applications")
public class ApplicationDo extends BaseDo {

    /** 应用编码，全局唯一 */
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    /** 应用名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 应用描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 应用生命周期状态（ACTIVE/INACTIVE） */
    @Column(name = "state", nullable = false, length = 16)
    private String state;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    @Column(name = "timeout", nullable = false)
    private int timeout;

    /** 配额预算 ID（预留） */
    @Column(name = "quota_budget_id")
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    @Column(name = "dashboard_id")
    private Long dashboardId;
}
