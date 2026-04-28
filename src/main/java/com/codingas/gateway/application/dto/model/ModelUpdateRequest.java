package com.codingas.gateway.adapter.admin.dto.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 更新模型请求
 */
@Data
public class ModelUpdateRequest {

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private Integer contextWindow;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Map<String, Boolean> capabilities;

    private Boolean enabled;
}
