package com.codingas.gateway.domain.security.gateway;

/**
 * IP 黑名单网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface IpBlockGateway {

    /**
     * 检查 IP 是否被封锁
     *
     * @param ipAddress IP 地址
     * @return 是否被封锁
     */
    boolean isBlocked(String ipAddress);

    /**
     * 封锁 IP
     *
     * @param ipAddress IP 地址
     * @param reason 封锁原因
     * @param blockedBy 封锁操作人
     * @param expiresAt 过期时间，null 表示永久封锁
     */
    void block(String ipAddress, String reason, Long blockedBy, java.time.Instant expiresAt);

    /**
     * 解封 IP
     *
     * @param ipAddress IP 地址
     */
    void unblock(String ipAddress);
}