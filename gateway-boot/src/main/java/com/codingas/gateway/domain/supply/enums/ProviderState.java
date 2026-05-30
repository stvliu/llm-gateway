package com.codingas.gateway.domain.supply.enums;

/**
 * 供应商状态枚举
 *
 * <p>企业内部 LLM Gateway 的供应商生命周期状态，采用简化设计。</p>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 * </pre>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>ACTIVE：正常运行，可接受请求</li>
 *   <li>INACTIVE：管理员停用，不接受请求（可恢复）</li>
 * </ul>
 */
public enum ProviderState {
    /** 正常运行，可接受请求 */
    ACTIVE,

    /** 已停用，不接受请求（可恢复） */
    INACTIVE;

    /**
     * 判断是否可接受请求
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}