package com.codingas.gateway.application.metadata.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新供应商元数据请求 DTO
 */
@Data
public class MetadataUpdateRequest {

    private String providerName;

    private Map<String, Object> providerConfig;

    private String description;

    private String iconUrl;

    private List<String> tags;
}