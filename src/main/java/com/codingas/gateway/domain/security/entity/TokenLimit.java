package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额实体
 *
 * <p>记录用户的 Token 使用限额和已使用量。</p>
 */
@Entity
@Table(name = "token_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLimit extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "monthly_limit", nullable = false)
    private Long monthlyLimit;

    @Column(name = "monthly_used", nullable = false)
    private Long monthlyUsed;

    @Column(name = "reset_at")
    private Instant resetAt;

    @Column(name = "enabled")
    private Boolean enabled;
}