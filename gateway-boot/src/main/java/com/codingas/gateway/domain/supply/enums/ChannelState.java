package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道生命周期状态枚举
 *
 * <p>替代原 Channel.State 内枚举，提取为独立顶层枚举，
 * 供 domain 层实体与 infrastructure 层 JPA 实体统一引用。</p>
 *
 * <ul>
 *   <li>PENDING — 待验证，不可路由</li>
 *   <li>ACTIVE — 启用，可路由、可计费</li>
 *   <li>SUSPENDED — 停用，不可路由</li>
 *   <li>DEPRECATED — 废弃，仍可路由（低优先级），仍计费</li>
 *   <li>RETIRED — 已退役，终态，不可路由</li>
 * </ul>
 */
public enum ChannelState {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEPRECATED,
    RETIRED;

    /**
     * 是否参与流量分配
     *
     * <p>ACTIVE 和 DEPRECATED 均可路由。</p>
     */
    public boolean isRoutable() {
        return this == ACTIVE || this == DEPRECATED;
    }

    /**
     * 是否为终态
     *
     * <p>RETIRED 为终态，不可再转换。</p>
     */
    public boolean isTerminal() {
        return this == RETIRED;
    }

    /**
     * 校验从当前状态到目标状态的转换是否合法
     *
     * <ul>
     *   <li>PENDING → ACTIVE</li>
     *   <li>ACTIVE → SUSPENDED, DEPRECATED</li>
     *   <li>SUSPENDED → ACTIVE, DEPRECATED, RETIRED</li>
     *   <li>DEPRECATED → RETIRED</li>
     *   <li>RETIRED → 无</li>
     * </ul>
     */
    public boolean canTransitionTo(ChannelState target) {
        return switch (this) {
            case PENDING    -> target == ACTIVE;
            case ACTIVE     -> target == SUSPENDED || target == DEPRECATED;
            case SUSPENDED  -> target == ACTIVE || target == DEPRECATED || target == RETIRED;
            case DEPRECATED -> target == RETIRED;
            case RETIRED    -> false;
        };
    }
}
