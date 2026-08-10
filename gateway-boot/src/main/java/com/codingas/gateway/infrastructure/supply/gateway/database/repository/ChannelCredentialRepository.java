/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelCredentialDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 渠道凭证 Repository
 */
public interface ChannelCredentialRepository extends JpaRepository<ChannelCredentialDo, Long> {

    List<ChannelCredentialDo> findByChannelId(Long channelId);
}