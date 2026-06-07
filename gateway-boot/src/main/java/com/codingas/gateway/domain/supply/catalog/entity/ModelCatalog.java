package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 模型目录实体
 *
 * <p>存储模型的固有规格信息，包括上下文窗口、能力标签、模态等。</p>
 * <p>capabilities 和 modalities 在实体中存储为 JSON 字符串，转换在 Infrastructure 层处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ModelCatalog extends BaseEntity {

    /** 模型标识 */
    private String modelName;

    /** 展示名 */
    private String displayName;

    /** 模型族 */
    private String modelFamily;

    /** 上下文窗口大小 */
    private Integer contextWindow;

    /** 最大输入 Token 数 */
    private Integer maxInputTokens;

    /** 最大输出 Token 数 */
    private Integer maxOutputTokens;

    /** 知识截止日期 */
    private String knowledgeCutoff;

    /** 能力标签 JSON：{vision, tool_use, streaming, ...} */
    private String capabilities;

    /** 模态列表 JSON：["text", "image", "audio"] */
    private String modalities;

    /** 同步时间 */
    private Instant syncedAt;

    /** 目录状态，默认 ACTIVE */
    private CatalogState state = CatalogState.ACTIVE;
}