package com.codingas.gateway.application.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用元数据请求 DTO
 */
@Data
public class ApplyMetadataRequest {

    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    private String channelName;

    private Integer channelPriority;
}