package com.codingas.gateway.domain.team.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 团队-渠道关联实体（多对多关系）
 * 
 * <p>定义团队可访问的渠道集合，作为权限基线。</p>
 * <p>用户 API Key 的渠道权限应为其所属团队允许渠道的子集。</p>
 */
@Data
@NoArgsConstructor
@Slf4j
public class TeamChannel {

    /** 团队 ID */
    private Long teamId;

    /** 渠道 ID */
    private Long channelId;

    /** 创建时间 */
    private Instant createdAt;

    public TeamChannel(Long teamId, Long channelId) {
        this.teamId = teamId;
        this.channelId = channelId;
        this.createdAt = Instant.now();
    }

    @Override
    public String toString() {
        return "TeamChannel{" +
                "teamId=" + teamId +
                ", channelId=" + channelId +
                '}';
    }
}
