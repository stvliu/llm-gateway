package com.codingas.gateway.infrastructure.model.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.model.enums.ModelState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 模型 DO
 *
 * <p>JPA 实体，对应数据库 models 表。</p>
 */
@Entity
@Table(name = "models", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider_id", "provider_model_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelDo extends BaseDo {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderDo provider;

    @Column(name = "provider_model_id", nullable = false, length = 128)
    private String providerModelId;

    @Column(name = "display_name", length = 256)
    private String displayName;

    @Column(name = "context_window")
    private Integer contextWindow;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "json")
    private Map<String, Boolean> capabilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ModelState state = ModelState.ACTIVE;

    /**
     * 渠道优先级（用于 FAILOVER 策略，值越小越优先）
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    /**
     * 渠道权重（用于 WEIGHTED 策略，加权随机选择）
     */
    @Column(name = "weight", nullable = false)
    private Integer weight = 100;
}
