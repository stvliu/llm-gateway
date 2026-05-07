package com.codingas.gateway.infrastructure.template.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider 模板 DO
 *
 * <p>JPA 实体，对应数据库 provider_templates 表。</p>
 */
@Entity
@Table(name = "provider_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderTemplateDo extends BaseDo {

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 32)
    private TemplateType templateType;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_config", nullable = false, columnDefinition = "json")
    private Map<String, Object> providerConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models_config", nullable = false, columnDefinition = "json")
    private List<Map<String, Object>> modelsConfig;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_name", length = 64)
    private String authorName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "market_status", nullable = false, length = 32)
    private MarketStatus marketStatus = MarketStatus.PRIVATE;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Builder.Default
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TemplateStatus status = TemplateStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 模板类型枚举
     */
    public enum TemplateType {
        OFFICIAL, USER
    }

    /**
     * 市场状态枚举
     */
    public enum MarketStatus {
        PRIVATE, PENDING, PUBLISHED, REJECTED
    }

    /**
     * 模板状态枚举
     */
    public enum TemplateStatus {
        ACTIVE, DISABLED
    }
}
