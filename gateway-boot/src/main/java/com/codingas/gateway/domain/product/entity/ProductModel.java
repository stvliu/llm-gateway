package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品-模型关联实体
 * <p>
 * 纯关联实体，仅承载产品与模型的多对多关系，不含定价信息。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ProductModel extends BaseEntity {

    private Long productId;

    private Long modelId;
}