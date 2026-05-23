package com.codingas.gateway.domain.team.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.team.enums.TeamState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 团队实体
 *
 * <p>用户组织单元，决定产品访问权限。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Team extends BaseEntity {

    /** 团队名称 */
    private String name;

    /** 团队描述 */
    private String description;

    /** 团队状态 */
    private TeamState state = TeamState.ACTIVE;

    /**
     * 检查团队是否可用
     */
    public boolean isAvailable() {
        return TeamState.ACTIVE.equals(state);
    }
}
