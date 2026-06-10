package com.codingas.gateway.application.init;

import com.codingas.gateway.infrastructure.config.GatewayProperties;

/**
 * 数据加载器接口
 *
 * <p>所有初始化加载器实现此接口，由 {@link DataInitializer} 按 {@link #getPhase()} 排序后依次驱动。</p>
 */
public interface DataLoader {

    /**
     * 当前加载器所属阶段（决定执行顺序）
     */
    InitPhase getPhase();

    /**
     * 是否启用。默认始终启用，子类可重写以根据配置控制开关。
     */
    default boolean isEnabled(GatewayProperties properties) {
        return true;
    }

    /**
     * 执行加载逻辑
     *
     * @param context 阶段间上下文，用于读取上游数据和写入下游数据
     */
    void load(DataLoadContext context);
}
