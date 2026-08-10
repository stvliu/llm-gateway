/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 渠道 Repository
 */
public interface ChannelRepository extends JpaRepository<ChannelDo, Long> {

    List<ChannelDo> findByProviderId(Long providerId);

    List<ChannelDo> findByState(String state);

    List<ChannelDo> findByIdIn(List<Long> ids);

    boolean existsByProviderIdAndName(Long providerId, String name);

    Optional<ChannelDo> findByProviderIdAndName(Long providerId, String name);

    List<ChannelDo> findByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);
}
