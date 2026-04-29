package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户角色关联实体
 *
 * <p>表示用户与角色的多对多关联关系。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class UserRole extends BaseEntity {

    private Long userId;

    private Long roleId;
}
