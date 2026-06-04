package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 套餐目录实体
 *
 * <p>存储套餐的目录信息，包括计费模式、端点列表、定价信息等。</p>
 * <p>endpoints 和 pricing 在实体中存储为 JSON 字符串，转换逻辑在 Infrastructure 层处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlanCatalog extends BaseEntity {

    /** 业务标识：volcengine_doubao_payg */
    private String planCode;

    /** 所属供应商代码 → ProviderCatalog */
    private String providerCode;

    /** 展示名 */
    private String planName;

    /** 计费模式：PAY_AS_YOU_GO / SUBSCRIPTION / PACKAGE */
    private BillingMode billingMode;

    /** 端点列表 JSON：[{protocol, url}, ...] */
    private String endpoints;

    /** 定价信息 JSON：[{providerModelId, inputPrice, outputPrice, ...}, ...] */
    private String pricing;

    /** 描述 */
    private String description;

    /** 目录数据来源，默认 BUILTIN */
    private CatalogSource source = CatalogSource.BUILTIN;

    /** 同步时间 */
    private Instant syncedAt;

    /** 目录状态，默认 ACTIVE */
    private CatalogState state = CatalogState.ACTIVE;
}
