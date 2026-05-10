package com.codingas.gateway.domain.template.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.template.enums.TemplateState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider 模板实体
 *
 * <p>预置大模型厂商配置模板，用户可一键创建 Provider。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ProviderTemplate extends BaseEntity {

    /** 模板唯一标识 */
    private String templateCode;

    /** 模板显示名称 */
    private String templateName;

    /** 模板类型 */
    private TemplateType templateType;

    /** Provider 类型 */
    private String providerType;

    /** Provider 配置（JSON） */
    private Map<String, Object> providerConfig;

    /** 模型列表配置（JSON） */
    private List<Map<String, Object>> modelsConfig;

    /** 创建者 ID（官方模板为 null） */
    private Long authorId;

    /** 创建者名称 */
    private String authorName;

    /** 市场状态 */
    private MarketState marketState = MarketState.PRIVATE;

    /** 发布时间 */
    private Instant publishAt;

    /** 使用次数 */
    private Integer downloadCount = 0;

    /** 标签列表 */
    private List<String> tags;

    /** 描述 */
    private String description;

    /** 图标 URL */
    private String iconUrl;

    /** 状态 */
    private TemplateState state = TemplateState.ACTIVE;

    /** 软删除时间 */
    private Instant deletedAt;

    /**
     * 检查模板是否可用
     */
    public boolean isAvailable() {
        return TemplateState.ACTIVE.equals(state) && deletedAt == null;
    }

    /**
     * 增加使用次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }
}
