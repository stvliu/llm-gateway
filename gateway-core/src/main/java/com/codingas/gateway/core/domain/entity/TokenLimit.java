package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Token 限额实体
 *
 * <p>支持四级 Token 限额: Team / User / API Key / User×Channel</p>
 */
@Entity
@Table(name = "token_limits")
@Getter
@Setter
public class TokenLimit extends BaseEntity {

    /**
     * 限额编码 (业务标识)
     */
    @Column(name = "limit_code", nullable = false, unique = true, length = 64)
    private String limitCode;

    /**
     * 所属团队 ID
     */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /**
     * 限额层级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    /**
     * 层级 ID (对应团队/用户/API Key ID)
     */
    @Column(name = "scope_id")
    private Long scopeId;

    /**
     * 用户 ID (scope_type 为 USER_CHANNEL 时必填)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 渠道 ID (scope_type 为 USER_CHANNEL 时必填)
     */
    @Column(name = "channel_id")
    private Long channelId;

    /**
     * 父限额 ID (层级限额的父限额)
     */
    @Column(name = "parent_limit_id")
    private Long parentLimitId;

    /**
     * 是否启用 Token 限额
     */
    @Column(name = "token_limit_enabled", nullable = false)
    private Boolean tokenLimitEnabled = true;

    /**
     * Token 限额总量 (NULL 表示不限)
     */
    @Column(name = "max_tokens", precision = 20, scale = 6)
    private BigDecimal maxTokens;

    /**
     * 周期类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 32)
    private PeriodType periodType = PeriodType.TOTAL;

    /**
     * 周内日期 (1-7, WEEKLY 时有效)
     */
    @Column(name = "period_day_of_week")
    private Integer periodDayOfWeek;

    /**
     * 月内日期 (1-31, MONTHLY 时有效)
     */
    @Column(name = "period_day_of_month")
    private Integer periodDayOfMonth;

    /**
     * 是否启用请求次数限额
     */
    @Column(name = "request_limit_enabled", nullable = false)
    private Boolean requestLimitEnabled = false;

    /**
     * 请求次数限额 (周期内最大请求次数)
     */
    @Column(name = "max_requests")
    private Integer maxRequests;

    /**
     * 超限动作
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "exceeded_action", length = 32)
    private ExceededAction exceededAction = ExceededAction.REJECT;

    /**
     * 降级切换模型 ID
     */
    @Column(name = "switch_model_id")
    private Long switchModelId;

    /**
     * 降级切换渠道 ID
     */
    @Column(name = "switch_channel_id")
    private Long switchChannelId;

    /**
     * 限额状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LimitStatus status = LimitStatus.ACTIVE;

    /**
     * 限额层级枚举
     */
    public enum ScopeType {
        /** 团队级 */
        TEAM,
        /** 用户级 */
        USER,
        /** API Key 级 */
        API_KEY,
        /** 用户×渠道级 */
        USER_CHANNEL
    }

    /**
     * 周期类型枚举
     */
    public enum PeriodType {
        /** 每天重置 */
        DAILY,
        /** 每周重置 */
        WEEKLY,
        /** 每月重置 */
        MONTHLY,
        /** 不重置 (累计总量限制) */
        TOTAL
    }

    /**
     * 超限动作枚举
     */
    public enum ExceededAction {
        /** 拒绝请求 */
        REJECT,
        /** 降级模型 */
        DOWNGRADE
    }

    /**
     * 限额状态枚举
     */
    public enum LimitStatus {
        /** 活跃 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }
}
