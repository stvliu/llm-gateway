package com.codingas.gateway.domain.supply.enums;

/**
 * 触发健康测试的来源。
 *
 * <p>仅 CARD / DRAWER 会持久化到 channel 表；PRECHECK 来自创建前预检工具，不写库。</p>
 *
 * <ul>
 *   <li>CARD：渠道卡片闪电图标触发</li>
 *   <li>DRAWER：详情抽屉"测试全部"触发</li>
 *   <li>PRECHECK：创建前预检工具触发</li>
 * </ul>
 */
public enum ChannelHealthSource {
    CARD,
    DRAWER,
    PRECHECK
}
