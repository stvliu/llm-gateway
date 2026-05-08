package com.codingas.gateway.application.template.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新模板请求
 */
@Data
public class TemplateUpdateRequest {

    private String templateName;
    private Map<String, Object> providerConfig;
    private List<Map<String, Object>> modelsConfig;
    private String description;
    private String iconUrl;
    private List<String> tags;
    private String status;
}
