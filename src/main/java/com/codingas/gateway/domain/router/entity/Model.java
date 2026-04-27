package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 模型实体
 *
 * <p>表示可用的 AI 模型。</p>
 */
@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Model extends BaseEntity {

    @Column(name = "model_code", nullable = false, unique = true, length = 128)
    private String modelCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "input_price", precision = 10, scale = 6)
    private BigDecimal inputPrice;

    @Column(name = "output_price", precision = 10, scale = 6)
    private BigDecimal outputPrice;

    @Column(name = "capabilities", columnDefinition = "TEXT")
    private String capabilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ModelStatus status;

    public enum ModelStatus {
        ACTIVE, INACTIVE, DEPRECATED
    }
}
