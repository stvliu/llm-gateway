package com.codingas.gateway.domain.model.enums;

/**
 * Provider API Key 状态枚举
 *
 * <p>企业内部 LLM Gateway 的提供商密钥生命周期状态，采用简化设计。</p>
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
 *   <li>ACTIVE：正常使用，可接受请求</li>
 *   <li>DISABLED：管理员禁用，不接受请求（可恢复）</li>
 *   <li>DELETED：已删除（终态）</li>
 * </ul>
 *
 * <h3>运行时健康状态</h3>
 * <p>速率限制、配额超限、降级等运行时状态由 {@link ProviderApiKeyHealth} 管理，
 * 不持久化到数据库，由熔断器组件自动管理。</p>
 */
public enum ProviderApiKeyState {
    /** 正常使用，可接受请求 */
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
