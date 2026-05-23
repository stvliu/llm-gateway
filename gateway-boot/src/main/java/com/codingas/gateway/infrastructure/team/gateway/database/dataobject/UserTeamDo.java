package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-团队关联数据对象
 */
@Data
@Entity
@Table(name = "user_teams")
@IdClass(UserTeamDo.UserTeamId.class)
public class UserTeamDo {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "role", length = 32)
    private String role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Data
    public static class UserTeamId implements java.io.Serializable {
        private Long userId;
        private Long teamId;
    }
}
