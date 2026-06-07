package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模型实例创建请求
 */
@Data
public class ModelInstanceCreateRequest {

    @NotNull(message = "模型 ID 不能为空")
    private Long modelId;

    /** 上游模型名，为空表示与 Model.modelName 相同 */
    private String upstreamModelName;

    /** 优先级（默认 100） */
    private Integer priority;

    /** 权重（默认 100） */
    private Integer weight;
}