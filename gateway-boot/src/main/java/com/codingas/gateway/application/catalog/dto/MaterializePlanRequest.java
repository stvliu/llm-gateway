package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 套餐物化请求（扩展版）
 *
 * <p>支持批量创建 API Key 凭证和自定义端点/模型配置。</p>
 */
@Getter
@Setter
public class MaterializePlanRequest {

    /** API Key 列表（批量创建凭证） */
    private List<String> apiKeys;

    /** 自定义端点列表（覆盖目录默认值） */
    private List<EndpointConfig> endpoints;

    /** 自定义模型列表（覆盖目录默认值） */
    private List<String> models;

    /** 渠道名称（可选，默认使用 planCode） */
    private String channelName;

    /**
     * 端点配置
     */
    @Getter
    @Setter
    public static class EndpointConfig {
        /** 协议类型：OPENAI / ANTHROPIC / GEMINI */
        private String protocol;
        /** 端点 URL */
        private String url;
    }
}
