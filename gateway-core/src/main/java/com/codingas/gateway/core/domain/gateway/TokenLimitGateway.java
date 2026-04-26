package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.TokenLimit;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Token 限额网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作 Token 限额。</p>
 */
public interface TokenLimitGateway {

    /**
     * 根据用户 ID 查找 Token 限额
     *
     * @param userId 用户 ID
     * @return 限额信息，不存在返回空
     */
    Optional<TokenLimit> findByUserId(Long userId);

    /**
     * 根据限额编码查找 Token 限额
     *
     * @param limitCode 限额编码
     * @return 限额信息，不存在返回空
     */
    Optional<TokenLimit> findByLimitCode(String limitCode);

    /**
     * 根据模型 ID 查找 Token 限额
     *
     * @param modelId 模型 ID
     * @return 限额信息，不存在返回空
     */
    Optional<TokenLimit> findByModelId(Long modelId);

    /**
     * 保存 Token 限额
     *
     * @param tokenLimit 限额实体
     * @return 保存后的实体
     */
    TokenLimit save(TokenLimit tokenLimit);

    /**
     * 更新已使用 Token 量
     *
     * @param limitCode 限额编码
     * @param usedTokens 已使用量
     */
    void updateUsedTokens(String limitCode, BigDecimal usedTokens);

    /**
     * 增加已使用 Token 量
     *
     * @param limitCode 限额编码
     * @param tokens 增加量
     */
    void addUsedTokens(String limitCode, BigDecimal tokens);
}
