package com.codingas.gateway.domain.team.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户-团队关联实体
 */
@Data
@DomainEntity
@Slf4j
public class UserTeam {

    /** 用户 ID */
    private Long userId;

    /** 团队 ID */
    private Long teamId;

    /** 团队角色（ADMIN / MEMBER） */
    private String role = "MEMBER";
}
