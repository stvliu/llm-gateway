package com.codingas.gateway.application.channel.dto;

import lombok.Data;

/**
 * 渠道模型关联响应
 */
@Data
public class ChannelModelResponse {

    private Long id;

    private Long channelId;

    private Long modelId;

    /** 供应商侧模型名称 */
    private String modelName;

    /** 模型展示名称 */
    private String displayName;

    /** 模型系列 */
    private String modelFamily;

    /** 上游模型名（为 null 表示与 modelName 相同） */
    private String upstreamModelName;

    /** 关联状态 */
    private String state;
}