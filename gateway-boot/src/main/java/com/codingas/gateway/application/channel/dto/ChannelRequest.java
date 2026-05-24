package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道创建/更新请求
 */
@Data
public class ChannelRequest {

    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 计费模式 */
    @NotBlank(message = "计费模式不能为空")
    private String billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;
}
