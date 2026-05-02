package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.domain.proxy.entity.SemanticCache;

import java.util.Optional;

/**
 * 语义缓存网关接口
 */
public interface SemanticCacheGateway {

    /**
     * 根据查询哈希查找缓存
     */
    Optional<SemanticCache> findByQueryHash(String queryHash);

    /**
     * 保存缓存
     */
    SemanticCache save(SemanticCache cache);

    /**
     * 清理过期缓存
     */
    void cleanExpired();
}
