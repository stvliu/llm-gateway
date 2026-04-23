package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 模型实体
 *
 * <p>表示某个 Provider 下的具体模型。</p>
 */
@Entity
@Table(name = "models")
@Getter
@Setter
public class Model extends BaseEntity {

    /**
     * 模型编码 (业务标识)
     */
    @Column(name = "model_code", nullable = false, unique = true, length = 128)
    private String modelCode;

    /**
     * 所属 Provider ID
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /**
     * Provider 侧的模型 ID (如 gpt-4o)
     */
    @Column(name = "provider_model_id", nullable = false, length = 128)
    private String providerModelId;

    /**
     * 显示名称
     */
    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    /**
     * 上下文窗口大小 (token 数)
     */
    @Column(name = "context_window")
    private Integer contextWindow;

    /**
     * 输入价格 (每 1M tokens)
     */
    @Column(name = "input_price", precision = 10, scale = 6)
    private BigDecimal inputPrice;

    /**
     * 输出价格 (每 1M tokens)
     */
    @Column(name = "output_price", precision = 10, scale = 6)
    private BigDecimal outputPrice;

    /**
     * 模型能力 (JSON: streaming, function_calling, vision 等)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private Map<String, Object> capabilities;

    /**
     * 模型状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ModelStatus status = ModelStatus.ACTIVE;

    /**
     * 模型状态枚举
     */
    public enum ModelStatus {
        /** 活跃 */
        ACTIVE,
        /** 已废弃 */
        DEPRECATED,
        /** 已删除 */
        DELETED
    }
}
