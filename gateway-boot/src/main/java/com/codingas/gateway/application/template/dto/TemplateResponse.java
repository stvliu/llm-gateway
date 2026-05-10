package com.codingas.gateway.application.template.dto;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 模板响应
 */
@Data
@Builder
public class TemplateResponse {

    private Long id;
    private String templateCode;
    private String templateName;
    private TemplateType templateType;
    private String providerType;
    private Map<String, Object> providerConfig;
    private List<Map<String, Object>> modelsConfig;
    private Long authorId;
    private String authorName;
    private MarketStatus marketStatus;
    private Instant publishAt;
    private Integer downloadCount;
    private List<String> tags;
    private String description;
    private String iconUrl;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer modelCount;
}
