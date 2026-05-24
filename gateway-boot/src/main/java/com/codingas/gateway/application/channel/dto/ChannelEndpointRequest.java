package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道端点创建/更新请求
 */
@Data
public class ChannelEndpointRequest {

    @NotNull(message = "协议类型不能为空")
    private String protocol;

    @NotBlank(message = "端点 URL 不能为空")
    private String endpointUrl;
}
