package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 产品实体
 *
 * <p>表示供应商提供的计费产品，包含定价和访问端点。</p>
 * <p>一个供应商可以有多个产品（如按量计费、Coding Plan、Token Plan）。</p>
 * <p>模型关联通过 ProductModel 承载，不在产品实体内持有模型列表。</p>
 * <p>业务逻辑（模型匹配、端点选择）由 {@link com.codingas.gateway.domain.product.service.ProductDomainService} 处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Product extends BaseEntity {

    /** 关联的供应商 ID */
    private Long providerId;

    /** 供应商名称（冗余，便于显示） */
    private String providerName;

    /** 产品名称 */
    private String name;

    /** 产品类型 */
    private ProductType productType;

    /** 多协议端点映射，key 为协议名，value 为 Base URL */
    private Map<String, String> endpoints;

    /** 输入价格（每百万 Token） */
    private BigDecimal inputPrice;

    /** 输出价格（每百万 Token） */
    private BigDecimal outputPrice;

    /** 推理价格（每百万 Token） */
    private BigDecimal reasoningPrice;

    /** 缓存读取价格（每百万 Token） */
    private BigDecimal cacheReadPrice;

    /** 缓存写入价格（每百万 Token） */
    private BigDecimal cacheWritePrice;

    /** 输入音频价格（每百万 Token） */
    private BigDecimal inputAudioPrice;

    /** 输出音频价格（每百万 Token） */
    private BigDecimal outputAudioPrice;

    /** 额度限制（Token 数），订阅产品专用 */
    private Long quotaLimit;

    /** 产品状态 */
    private ProductState state = ProductState.ACTIVE;

    /**
     * 检查产品是否可用
     */
    public boolean isAvailable() {
        return ProductState.ACTIVE.equals(state);
    }
}
