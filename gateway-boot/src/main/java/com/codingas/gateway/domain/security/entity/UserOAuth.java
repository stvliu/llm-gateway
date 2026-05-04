package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户 OAuth 身份实体
 *
 * <p>存储用户的第三方 OAuth 身份信息。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class UserOAuth extends BaseEntity {

    private Long userId;

    private String provider;

    private String providerUserId;

    private String accessToken;

    private String refreshToken;

    private String scope;
}
