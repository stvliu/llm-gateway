package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模型实例创建请求
 *
 * <p>注意：channelId 由适配层（Controller/gRPC stub）从协议上下文中提取并填充，
 * 请求体本身不包含此字段。</p>
 */
@Data
public class ModelInstanceCreateRequest {

    /** 渠道 ID（适配层填充） */
    private Long channelId;

    @NotNull(message = "模型 ID 不能为空")
    private Long modelId;

    /** 上游模型名，为空表示与 Model.modelName 相同 */
    private String upstreamModelName;

    /** 优先级（默认 100） */
    private Integer priority;

    /** 权重（默认 100） */
    private Integer weight;
}