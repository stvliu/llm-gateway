package com.codingas.gateway.application.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加团队成员请求
 */
@Data
public class AddTeamMemberRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotBlank(message = "角色不能为空")
    private String role;
}