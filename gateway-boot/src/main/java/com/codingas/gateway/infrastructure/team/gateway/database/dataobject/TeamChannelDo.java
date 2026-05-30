package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 团队-渠道关联数据对象
 */
@Entity
@Table(name = "team_channels")
@IdClass(TeamChannelDo.TeamChannelId.class)
public class TeamChannelDo {

    @Id
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Id
    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class TeamChannelId implements java.io.Serializable {
        private Long teamId;
        private Long channelId;
        public Long getTeamId() { return teamId; }
        public void setTeamId(Long teamId) { this.teamId = teamId; }
        public Long getChannelId() { return channelId; }
        public void setChannelId(Long channelId) { this.channelId = channelId; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TeamChannelId that)) return false;
            return java.util.Objects.equals(teamId, that.teamId) && java.util.Objects.equals(channelId, that.channelId);
        }
        @Override
        public int hashCode() { return java.util.Objects.hash(teamId, channelId); }
    }
}
