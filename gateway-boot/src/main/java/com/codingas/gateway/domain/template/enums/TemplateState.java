package com.codingas.gateway.domain.template.enums;

/**
 * 模板状态枚举
 *
 * <p>模板有两个独立的状态维度：</p>
 * <ul>
 *   <li>TemplateState：模板本身的启用/禁用状态</li>
 *   <li>MarketStatus：模板在市场中的发布状态</li>
 * </ul>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * DELETED（终态）
 * </pre>
 */
public enum TemplateState {
    /** 正常运行 */
    ACTIVE,

    /** 已禁用 */
    DISABLED,

    /** 已删除（终态） */
    DELETED;

    /**
     * 判断是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }

    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == DELETED;
    }
}
