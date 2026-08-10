/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelEndpointDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点 Repository
 */
public interface ChannelEndpointRepository extends JpaRepository<ChannelEndpointDo, Long> {

    List<ChannelEndpointDo> findByChannelId(Long channelId);

    Optional<ChannelEndpointDo> findByChannelIdAndProtocol(Long channelId, Protocol protocol);
}