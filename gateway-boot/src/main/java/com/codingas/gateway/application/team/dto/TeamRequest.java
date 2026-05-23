package com.codingas.gateway.application.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 团队创建/更新请求
 */
@Data
public class TeamRequest {

    @NotBlank(message = "团队名称不能为空")
    private String name;

    private String description;
}
