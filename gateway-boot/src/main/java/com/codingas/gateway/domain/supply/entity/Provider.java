package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 供应商实体
 *
 * <p>表示 AI 模型服务提供商，如 OpenAI、Anthropic、智谱等。</p>
 * <p>品牌标识使用 name 字段，code 字段为程序标识。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Provider extends BaseEntity {

    /** 程序标识（如 "openai", "anthropic", "zhipu"） */
    private String code;

    /** 显示名（如 "OpenAI", "智谱AI"） */
    private String name;

    private String logoUrl;

    private String websiteUrl;

    private String description;

    /** API 文档 URL */
    private String apiDocUrl;

    /** 路由优先级（数值越小优先级越高） */
    private Integer priority;

    

    /**
     * 检查供应商是否可用（供应商始终可用）
     */
    public boolean isAvailable() {
        return true;
    }
}