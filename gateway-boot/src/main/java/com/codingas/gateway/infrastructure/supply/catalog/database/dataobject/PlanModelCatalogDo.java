package com.codingas.gateway.infrastructure.supply.catalog.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 套餐-模型关联目录数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plan_model_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_code", "model_name"}))
public class PlanModelCatalogDo extends BaseDo {

    @Column(name = "plan_code", nullable = false, length = 128)
    private String planCode;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}
