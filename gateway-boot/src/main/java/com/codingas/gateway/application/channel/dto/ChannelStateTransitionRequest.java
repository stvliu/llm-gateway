package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道状态转换请求
 */
@Data
public class ChannelStateTransitionRequest {

    /** 目标状态（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED） */
    @NotBlank(message = "目标状态不能为空")
    private String targetState;

    /** 转换原因（可选，用于审计） */
    private String reason;
}
