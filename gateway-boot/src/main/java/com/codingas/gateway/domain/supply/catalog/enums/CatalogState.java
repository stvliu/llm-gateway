package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 目录状态
 */
public enum CatalogState {

    /** 正常可用 */
    ACTIVE,

    /** 已下线（上游数据源中消失） */
    DEPRECATED
}
