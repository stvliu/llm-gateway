package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 目录状态枚举
 */
public enum CatalogState {

    /** 正常可用 */
    ACTIVE,

    /** 已禁用 */
    DISABLED,

    /** 已废弃（上游数据源中消失） */
    DEPRECATED,

    /** 已删除（逻辑删除） */
    DELETED
}