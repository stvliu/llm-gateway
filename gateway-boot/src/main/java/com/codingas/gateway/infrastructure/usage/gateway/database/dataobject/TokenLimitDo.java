package com.codingas.gateway.infrastructure.usage.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.usage.enums.ExceededAction;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额 DO
 *
 * <p>JPA 实体，对应数据库 token_limits 表。</p>
 */
@Entity
@Table(name = "token_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "provider_id", "model_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLimitDo extends BaseDo {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "model_id")
    private Long modelId;

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

    @Column(name = "switch_model_id")
    private Long switchModelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TokenLimitStatus status = TokenLimitStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum LimitType {
        SYSTEM_DEFAULT,
        USER_CUSTOM
    }

    public enum TokenLimitStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}
