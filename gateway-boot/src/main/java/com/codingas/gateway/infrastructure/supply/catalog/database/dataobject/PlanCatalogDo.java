package com.codingas.gateway.infrastructure.supply.catalog.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 套餐目录数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plan_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "plan_code"))
public class PlanCatalogDo extends BaseDo {

    @Column(name = "plan_code", nullable = false, unique = true, length = 128)
    private String planCode;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "plan_name", nullable = false, length = 128)
    private String planName;

    @Column(name = "billing_mode", nullable = false, length = 32)
    private String billingMode;

    @Column(name = "endpoints", columnDefinition = "TEXT")
    private String endpoints;

    @Column(name = "pricing", columnDefinition = "TEXT")
    private String pricing;

    @Column(name = "description", length = 1024)
    private String description;
}
