package com.codingas.gateway.application.template.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板导出 DTO（用于 JSON 序列化）
 */
@Data
@Builder
public class TemplateExportDto {

    private String templateCode;

    private String templateName;

    private String providerType;

    private Map<String, Object> providerConfig;

    private List<Map<String, Object>> modelsConfig;

    private String description;

    private String iconUrl;

    private List<String> tags;
}