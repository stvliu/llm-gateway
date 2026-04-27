package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.TokenLimit;

/**
 * Token 限额网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface TokenLimitGateway {

    /**
     * 根据用户 ID 查找限额
     *
     * @param userId 用户 ID
     * @return 限额信息，不存在返回 null
     */
    TokenLimit findByUserId(Long userId);

    /**
     * 保存限额
     *
     * @param tokenLimit 限额实体
     * @return 保存后的实体
     */
    TokenLimit save(TokenLimit tokenLimit);

    /**
     * 扣减已使用量
     *
     * @param userId 用户 ID
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
    void deductUsage(Long userId, Long inputTokens, Long outputTokens);
}