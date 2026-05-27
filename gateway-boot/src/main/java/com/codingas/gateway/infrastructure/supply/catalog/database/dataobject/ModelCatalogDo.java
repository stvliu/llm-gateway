package com.codingas.gateway.infrastructure.supply.catalog.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 模型目录数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "model_name"))
public class ModelCatalogDo extends BaseDo {

    @Column(name = "model_name", nullable = false, unique = true, length = 128)
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

    @Column(name = "knowledge_cutoff", length = 32)
    private String knowledgeCutoff;

    @Column(name = "capabilities", columnDefinition = "TEXT")
    private String capabilities;

    @Column(name = "modalities", columnDefinition = "TEXT")
    private String modalities;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}