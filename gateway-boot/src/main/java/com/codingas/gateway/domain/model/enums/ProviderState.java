package com.codingas.gateway.domain.model.enums;

/**
 * 提供商状态枚举
 *
 * <p>企业内部 LLM Gateway 的提供商生命周期状态，采用简化设计。</p>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ DISABLED
 *    ↓
 * DELETED（终态）
 * </pre>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>ACTIVE：正常运行，可接受请求</li>
 *   <li>DISABLED：管理员禁用，不接受请求（可恢复）</li>
 *   <li>DELETED：已删除（终态）</li>
 * </ul>
 */
public enum ProviderState {
    /** 正常运行，可接受请求 */
    ACTIVE,

    /** 已禁用，不接受请求（可恢复） */
    DISABLED,

    /** 已删除（终态） */
    DELETED;

    /**
     * 判断是否可接受请求
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
