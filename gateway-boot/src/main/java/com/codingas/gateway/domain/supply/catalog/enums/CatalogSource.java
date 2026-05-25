package com.codingas.gateway.domain.supply.catalog.enums;

import lombok.Getter;

/**
 * 目录数据来源
 *
 * 优先级（低→高）：BUILTIN < MODELS_DEV < PROVIDER_API < MANUAL < OVERRIDE
 * 低优先级不可覆盖高优先级，同优先级可互相覆盖。
 */
@Getter
public enum CatalogSource {

    BUILTIN(0),
    MODELS_DEV(10),
    PROVIDER_API(20),
    MANUAL(30),
    OVERRIDE(40);

    private final int priority;

    CatalogSource(int priority) {
        this.priority = priority;
    }

    /** 当前 source 是否可以覆盖目标 source */
    public boolean canOverride(CatalogSource target) {
        return this.priority >= target.priority;
    }
}
