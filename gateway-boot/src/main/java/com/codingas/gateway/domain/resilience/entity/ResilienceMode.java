package com.codingas.gateway.domain.resilience.entity;

/**
 * 容灾模式档位枚举
 *
 * <p>管理员面向的容灾模式三档位，覆盖五场景容灾诉求。
 * 档位语义见 design.md D5 与 {@code docs/容灾管理范式.md} 第四节。</p>
 *
 * <ul>
 *   <li>STANDARD（标准）— L1 全开 + L2 浅降级（深度 2）。通用默认，平衡可用性与质量。</li>
 *   <li>STRICT（严格）— L1 全开 + L2 关闭。不可降级场景，宁可报错不可换模型
 *       （对应 Claude Code/CodeX）。</li>
 *   <li>AGGRESSIVE（激进）— L1 全开 + L2 深降级（深度 3）+ 短超时 + 就近。
 *       可用性优先，质量次之（对应客服/HelpDesk）。</li>
 * </ul>
 *
 * <p>BATCH（批量）为 STANDARD 的 QUEUED 转移变体，不单列档位以保持三档简洁，
 * 由「容灾模式=STANDARD + 高级里切 transferMode=QUEUED」实现。</p>
 */
public enum ResilienceMode {
    /** 标准档位：L1 全开 + L2 浅降级（深度 2） */
    STANDARD,
    /** 严格档位：L1 全开 + L2 关闭，不可降级 */
    STRICT,
    /** 激进档位：L1 全开 + L2 深降级（深度 3）+ 短超时 + 就近 */
    AGGRESSIVE
}
