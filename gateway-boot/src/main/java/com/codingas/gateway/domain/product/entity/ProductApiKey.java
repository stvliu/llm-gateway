package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 产品 API Key 实体
 *
 * <p>供应商侧认证密钥，用于调用供应商 API。</p>
 * <p>一个产品可配置多个密钥，支持密钥轮换、负载均衡和故障转移。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class ProductApiKey extends BaseEntity {

    /** 关联的产品 ID */
    private Long productId;

    /** 密钥名称 */
    private String name;

    /** Key 明文（创建时设置，查询时由基础设施层解密填充） */
    private String apiKeyPlain;

    /** Key 前缀，用于识别 */
    private String apiKeyPrefix;


    /** 描述 */
    private String description;

    /** 负载均衡权重 */
    private Integer weight = 1;

    /** 故障转移优先级（数值越小优先级越高） */
    private Integer priority = 1;

    /** 密钥状态 */
    private ProductApiKeyState state = ProductApiKeyState.ACTIVE;

    /** 最后使用时间 */
    private Instant lastUsedAt;

    /**
     * 检查密钥是否可用
     */
    public boolean isAvailable() {
        return state.isAvailable();
    }
}
