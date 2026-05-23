package com.codingas.gateway.infrastructure.product.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品-模型关联 DO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_models", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pm_product_model", columnNames = {"product_id", "model_id"})
})
public class ProductModelDo extends BaseDo {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;
}