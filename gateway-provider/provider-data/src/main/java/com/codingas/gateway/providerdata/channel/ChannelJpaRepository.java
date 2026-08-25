/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.providerdata.channel;

import com.codingas.gateway.provider.model.BillingMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 渠道 Repository
 */
public interface ChannelJpaRepository extends JpaRepository<ChannelDo, Long> {

    List<ChannelDo> findByProviderId(Long providerId);

    List<ChannelDo> findByState(String state);

    List<ChannelDo> findByIdIn(List<Long> ids);

    boolean existsByProviderIdAndName(Long providerId, String name);

    Optional<ChannelDo> findByProviderIdAndName(Long providerId, String name);

    List<ChannelDo> findByProviderIdAndBillingMode(Long providerId, BillingMode billingMode);
}
