package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额实体
 *
 * <p>用户级别 Token 使用限额，支持周期重置。</p>
 */
@Entity
@Table(name = "token_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "provider_id", "model_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLimit extends BaseEntity {

    @Column(name = "limit_code", nullable = false, unique = true, length = 64)
    private String limitCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Model model;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    private LimitType limitType = LimitType.USER_CUSTOM;

    @Column(name = "max_tokens", precision = 20, scale = 6)
    private BigDecimal maxTokens;

    @Column(name = "used_tokens", precision = 20, scale = 6)
    private BigDecimal usedTokens = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType = PeriodType.MONTHLY;

    @Column(name = "period_day_of_week")
    private Integer periodDayOfWeek;

    @Column(name = "period_day_of_month")
    private Integer periodDayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "exceeded_action", nullable = false)
    private ExceededAction exceededAction = ExceededAction.REJECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "switch_model_id")
    private Model switchModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TokenLimitStatus status = TokenLimitStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum LimitType {
        /** 系统默认 */
        SYSTEM_DEFAULT,
        /** 用户自定义 */
        USER_CUSTOM
    }

    public enum TokenLimitStatus {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }
}