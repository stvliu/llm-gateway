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
}