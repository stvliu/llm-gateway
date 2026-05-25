package com.codingas.gateway.domain.quota.gateway;

import com.codingas.gateway.domain.usage.entity.TokenLimit;

import java.util.List;
import java.util.Optional;

/**
 * Token 限额网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface TokenLimitGateway {

    /**
     * 保存限额
     *
     * @param tokenLimit 限额实体
     * @return 保存后的实体
     */
    TokenLimit save(TokenLimit tokenLimit);

    /**
     * 根据 ID 查找限额
     *
     * @param id 限额 ID
     * @return 限额信息，不存在返回空
     */
    Optional<TokenLimit> findById(Long id);

    /**
     * 根据用户 ID 查找限额列表
     *
     * @param userId 用户 ID
     * @return 限额列表
     */
    List<TokenLimit> findByUserId(Long userId);

    /**
     * 查询所有限额
     *
     * @return 限额列表
     */
    List<TokenLimit> findAll();

    /**
     * 统计限额总数
     *
     * @return 限额数量
     */
    long count();

    /**
     * 删除限额
     *
     * @param tokenLimit 限额实体
     */
    void delete(TokenLimit tokenLimit);


    /**
     * 扣减已使用量
     *
     * @param userId 用户 ID
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
    void deductUsage(Long userId, Long inputTokens, Long outputTokens);
}