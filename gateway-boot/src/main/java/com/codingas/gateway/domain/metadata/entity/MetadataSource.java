package com.codingas.gateway.domain.metadata.entity;

/**
 * 模型元数据来源
 * <p>
 * 控制同步时的覆盖策略：
 * - BUILTIN / MODELS_DEV：可被 Models.dev 同步覆盖
 * - MANUAL：手动录入，同步时跳过
 * - OVERRIDE：用户覆盖，同步时跳过
 * - PROVIDER_API：供应商 API 获取，同步时跳过
 * </p>
 */
public enum MetadataSource {

    /**
     * 内置元数据（从 classpath JSON 加载）
     */
    BUILTIN,

    /**
     * Models.dev 社区数据源同步
     */
    MODELS_DEV,

    /**
     * 供应商 API 实时获取
     */
    PROVIDER_API,

    /**
     * 手动录入
     */
    MANUAL,

    /**
     * 用户覆盖（基于同步数据修改）
     */
    OVERRIDE
}
