package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 团队实体
 *
 * <p>团队是 LLM-Gateway 的隔离边界，替代传统的租户概念。</p>
 *
 * @see <a href="https://docs.llm-gateway.dev/architecture#team-isolation">团队隔离</a>
 */
@Entity
@Table(name = "teams")
@Getter
@Setter
public class Team extends BaseEntity {

    /**
     * 团队编码 (业务标识)
     */
    @Column(name = "team_code", nullable = false, unique = true, length = 64)
    private String teamCode;

    /**
     * 团队名称
     */
    @Column(name = "team_name", nullable = false, length = 128)
    private String teamName;

    /**
     * 描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 团队管理员 ID
     */
    @Column(name = "admin_id")
    private Long adminId;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TeamStatus status = TeamStatus.ACTIVE;

    /**
     * 团队状态枚举
     */
    public enum TeamStatus {
        /** 活跃 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }
}
