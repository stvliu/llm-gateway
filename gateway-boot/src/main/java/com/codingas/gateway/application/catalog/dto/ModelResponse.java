package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 模型响应 DTO
 *
 * <p>用于模型目录查询 API 的响应。</p>
 * <p>原 ModelCatalogResponse，合并 ModelCatalog 到 Model 后重命名。</p>
 */
@Getter
@Builder
public class ModelResponse {

    /** 模型名称 */
    private final String modelName;

    /** 展示名称 */
    private final String displayName;

    /** 所属供应商编码 */
    private final String providerCode;

    /** 模型能力标签 */
    private final List<String> capabilities;

    /** 上下文窗口 */
    private final Integer contextWindow;

    /** 最大输出 Token */
    private final Integer maxOutputTokens;

    /** 是否已物化 */
    private final Boolean materialized;
}
