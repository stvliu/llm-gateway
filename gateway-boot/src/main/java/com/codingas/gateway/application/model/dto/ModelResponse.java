package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.domain.model.enums.ModelState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 模型响应
 */
@Data
public class ModelResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String providerModelId;
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private ModelState state;

    /**
     * 渠道优先级（用于 FAILOVER 策略，值越小越优先）
     */
    private Integer priority;

    /**
     * 渠道权重（用于 WEIGHTED 策略，加权随机选择）
     */
    private Integer weight;

    private Instant createdAt;
    private Instant updatedAt;
}