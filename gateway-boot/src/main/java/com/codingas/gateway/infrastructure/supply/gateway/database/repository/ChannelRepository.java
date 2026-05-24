package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 渠道 Repository
 */
public interface ChannelRepository extends JpaRepository<ChannelDo, Long> {

    List<ChannelDo> findByProviderId(Long providerId);

    List<ChannelDo> findByProtocol(String protocol);

    List<ChannelDo> findByState(String state);

    List<ChannelDo> findByProviderIdAndProtocolAndState(Long providerId, String protocol, String state);

    List<ChannelDo> findByIdIn(List<Long> ids);

    boolean existsByProviderIdAndName(Long providerId, String name);

    List<ChannelDo> findByProviderIdAndBillingMode(Long providerId, String billingMode);
}