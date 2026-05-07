package com.codingas.gateway.application.template.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 应用模板结果
 */
@Data
@Builder
public class ApplyTemplateResult {

    private Long providerId;
    private String providerName;
    private Long channelId;
    private String channelName;
    private List<Long> modelIds;
    private List<String> modelNames;
    private Instant createdAt;
}