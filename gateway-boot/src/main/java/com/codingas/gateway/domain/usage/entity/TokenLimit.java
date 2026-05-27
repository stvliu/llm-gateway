package com.codingas.gateway.domain.usage.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.domain.usage.enums.ExceededAction;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.iam.entity.User;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额实体
 *
 * <p>用户级别 Token 使用限额，支持周期重置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class TokenLimit extends BaseEntity {

    private User user;

    private Provider provider;

    private ModelSpec model;

    private LimitType limitType = LimitType.USER_CUSTOM;

    private BigDecimal maxTokens;

    private BigDecimal usedTokens = BigDecimal.ZERO;

    private PeriodType periodType = PeriodType.MONTHLY;

    private Integer periodDayOfWeek;

    private Integer periodDayOfMonth;

    private ExceededAction exceededAction = ExceededAction.REJECT;

    private ModelSpec switchModel;

    private TokenLimitState state = TokenLimitState.ACTIVE;

    private Instant deletedAt;

    public enum LimitType {
        /** 系统默认 */
        SYSTEM_DEFAULT,
        /** 用户自定义 */
        USER_CUSTOM
    }

    public enum TokenLimitState {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED
    }

    /**
     * 检查是否超限
     */
    public boolean isExceeded() {
        return usedTokens.compareTo(maxTokens) >= 0;
    }
}
