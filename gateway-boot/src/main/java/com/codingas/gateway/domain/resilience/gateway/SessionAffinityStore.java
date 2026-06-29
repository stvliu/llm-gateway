package com.codingas.gateway.domain.resilience.gateway;

/**
 * 会话亲和存储接口
 *
 * <p>管理 sessionId → channelId 的亲和映射，支持读写和主动清除。
 * TTL 过期后 get 返回 null，表示不亲和。
 * 标识缺失（null sessionId）时 get 返回 null，put 不存储。</p>
 *
 * <p>双实现：{@code InMemorySessionAffinityStore}（开发/测试环境）、
 * {@code RedisSessionAffinityStore}（生产环境）。</p>
 */
public interface SessionAffinityStore {

    /**
     * 按 sessionId 获取绑定的 channelId
     *
     * @param sessionId 会话标识；为 null 时返回 null（不亲和）
     * @return 绑定的 channelId；不存在或已过期时返回 null（不亲和）
     */
    Long get(String sessionId);

    /**
     * 绑定 sessionId → channelId，设置 TTL
     *
     * @param sessionId 会话标识；为 null 时不存储
     * @param channelId 渠道 ID
     */
    void put(String sessionId, Long channelId);

    /**
     * 主动清除 sessionId 的亲和绑定
     *
     * @param sessionId 会话标识；不存在的 sessionId 不抛异常
     */
    void evict(String sessionId);
}
