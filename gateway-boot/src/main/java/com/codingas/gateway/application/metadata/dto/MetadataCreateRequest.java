package com.codingas.gateway.application.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建供应商元数据请求 DTO
 */
@Data
public class MetadataCreateRequest {

    @NotBlank(message = "供应商标识不能为空")
    private String providerId;

    @NotBlank(message = "供应商名称不能为空")
    private String providerName;

    @NotBlank(message = "供应商类型不能为空")
    private String providerType;

    private Map<String, Object> providerConfig;

    private String description;

    private String iconUrl;

    private List<String> tags;
}