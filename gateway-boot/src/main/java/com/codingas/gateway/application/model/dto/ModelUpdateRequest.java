package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.domain.model.enums.ModelState;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 更新模型请求
 */
@Data
public class ModelUpdateRequest {

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private Integer contextWindow;

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
}
