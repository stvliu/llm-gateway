package com.codingas.gateway.application.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用模板请求
 */
@Data
public class ApplyTemplateRequest {

    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    /** 渠道名称，不填则使用模板名称 */
    private String channelName;

    /** 渠道优先级，默认 100 */
    private Integer channelPriority = 100;
}