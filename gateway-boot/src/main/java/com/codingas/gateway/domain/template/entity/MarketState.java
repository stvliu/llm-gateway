package com.codingas.gateway.domain.template.entity;

/**
 * 模板市场状态枚举
 */
public enum MarketState {
    /** 私有，仅创建者可见 */
    PRIVATE,
    /** 待审核 */
    PENDING,
    /** 已发布到公共市场 */
    PUBLISHED,
    /** 审核拒绝 */
    REJECTED
}
