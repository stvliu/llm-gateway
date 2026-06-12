package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * 模型数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "models")
public class ModelDo extends BaseDo {


    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

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

    /** 知识截止日期 */
    @Column(name = "knowledge_cutoff", length = 32)
    private String knowledgeCutoff;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private Map<String, Boolean> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modalities", columnDefinition = "jsonb")
    private List<String> modalities;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "scheduled_retired_at")
    private Instant scheduledRetiredAt;

    @Column(name = "deprecation_message", length = 512)
    private String deprecationMessage;

    @Column(name = "state", nullable = false)
    private String state;
}