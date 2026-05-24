package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;

import java.util.List;
import java.util.Optional;

/**
 * 渠道持久化接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ChannelGateway {

    /**
     * 保存渠道
     */
    Channel save(Channel channel);

    /**
     * 根据 ID 查找渠道
     */
    Optional<Channel> findById(Long id);

    /**
     * 根据供应商 ID 查找渠道
     */
    List<Channel> findByProviderId(Long providerId);

    /**
     * 根据协议类型查找渠道
     */
    List<Channel> findByProtocol(Protocol protocol);

    /**
     * 查找活跃渠道
     */
    List<Channel> findAllActive();

    /**
     * 查找指定供应商和协议的活跃渠道
     */
    List<Channel> findActiveByProviderIdAndProtocol(Long providerId, Protocol protocol);

    /**
     * 查询所有渠道
     */
    List<Channel> findAll();

    /**
     * 根据渠道 ID 查找路由上下文
     */
    Optional<RoutingContext> findRoutingContext(Long channelId);

    /**
     * 删除渠道
     */
    void deleteById(Long id);

    /**
     * 批量查找渠道
     */
    List<Channel> findByIds(List<Long> ids);

    /**
     * 检查供应商下是否存在同名渠道
     */
    boolean existsByProviderIdAndName(Long providerId, String name);

    /**
     * 根据供应商ID和计费模式查找渠道
     */
    List<Channel> findByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);
}