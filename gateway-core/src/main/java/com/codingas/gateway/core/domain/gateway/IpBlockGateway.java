package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.IpBlocklist;

import java.util.List;

/**
 * IP 黑名单网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作 IP 黑名单。</p>
 */
public interface IpBlockGateway {

    /**
     * 根据 IP 地址查找黑名单记录
     *
     * @param ipAddress IP 地址
     * @return 黑名单记录，不存在返回空
     */
    IpBlocklist findByIpAddress(String ipAddress);

    /**
     * 检查 IP 是否在黑名单中
     *
     * @param ipAddress IP 地址
     * @return 是否在黑名单中
     */
    boolean isBlocked(String ipAddress);

    /**
     * 保存 IP 黑名单记录
     *
     * @param ipBlocklist 黑名单记录
     * @return 保存后的实体
     */
    IpBlocklist save(IpBlocklist ipBlocklist);

    /**
     * 删除 IP 黑名单记录
     *
     * @param ipAddress IP 地址
     */
    void deleteByIpAddress(String ipAddress);

    /**
     * 查询所有活跃的黑名单记录
     *
     * @return 黑名单记录列表
     */
    List<IpBlocklist> findAllActive();
}
