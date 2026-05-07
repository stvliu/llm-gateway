package com.codingas.gateway.application.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建模板请求
 */
@Data
public class TemplateCreateRequest {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "Provider 类型不能为空")
    private String providerType;

    @NotNull(message = "Provider 配置不能为空")
    private Map<String, Object> providerConfig;

    @NotNull(message = "模型配置不能为空")
    private List<Map<String, Object>> modelsConfig;

    private String description;
    private String iconUrl;
    private List<String> tags;
}
