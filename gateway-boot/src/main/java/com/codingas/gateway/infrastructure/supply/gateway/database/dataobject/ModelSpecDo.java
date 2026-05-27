package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * 模型规格数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_specs")
public class ModelSpecDo extends BaseDo {

    
    @Column(name = "provider_model_id", nullable = false, length = 128)
    private String providerModelId;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "model_family", length = 64)
    private String modelFamily;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_input_tokens")
    private Integer maxInputTokens;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "weight")
    private Integer weight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private Map<String, Boolean> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modalities", columnDefinition = "jsonb")
    private List<String> modalities;

    @Column(name = "state", nullable = false)
    private String state;
}