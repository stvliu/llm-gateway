package com.codingas.gateway.application.team.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队响应
 */
@Data
public class TeamResponse {

    private Long id;

    private String name;

    private String description;

    private String state;

    /** 成员数（列表接口填充，避免查询完整成员列表） */
    private Integer memberCount;

    private List<MemberResponse> members;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    public static class MemberResponse {
        private Long userId;
        private String role;
    }
}
