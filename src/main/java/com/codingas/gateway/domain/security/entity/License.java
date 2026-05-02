package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;

/**
 * 许可证实体
 *
 * <p>控制系统功能授权。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class License extends BaseEntity {

    private String licenseCode;

    private String licenseeName;

    private String licenseType;

    private Instant issuedAt;

    private Instant expiresAt;

    private Integer maxTeams;

    private Integer maxUsers;

    private Integer maxRequestsPerDay;

    private Set<String> features;

    private LicenseStatus status = LicenseStatus.ACTIVE;

    public enum LicenseStatus {
        /** 活跃 */
        ACTIVE,
        /** 已过期 */
        EXPIRED,
        /** 已撤销 */
        REVOKED
    }

    /**
     * 检查许可证是否有效
     */
    public boolean isValid() {
        if (!LicenseStatus.ACTIVE.equals(status)) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否包含指定功能
     */
    public boolean hasFeature(String feature) {
        return features != null && features.contains(feature);
    }
}
