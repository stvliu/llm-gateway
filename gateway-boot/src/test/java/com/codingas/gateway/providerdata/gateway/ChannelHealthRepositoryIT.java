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
package com.codingas.gateway.providerdata.gateway;

import com.codingas.gateway.boot.GatewayApplication;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channel 健康字段持久化集成测试。
 *
 * <p>验证 Gateway / Repository 层是否正确透传 last_health_check_at /
 * last_health_status / last_health_source 三个字段（往返保存与读取）。</p>
 */
@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles("test")
@Transactional
class ChannelHealthRepositoryIT {

    @Autowired
    private ChannelGateway channelGateway;

    @Autowired
    private ProviderGateway providerGateway;

    @Test
    void 应能持久化与读回三个健康字段() {
        // 准备：先建一个 Provider 满足渠道外键约束
        Provider provider = new Provider();
        provider.setCode("test-health-provider-" + System.nanoTime());
        provider.setName("测试供应商");
        Provider savedProvider = providerGateway.save(provider);

        // 构造合法的 Channel，并设置三个健康字段
        Channel channel = new Channel();
        channel.setProviderId(savedProvider.getId());
        channel.setName("health-test-channel");
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.PENDING);
        Instant checkAt = Instant.parse("2026-06-13T10:00:00Z");
        channel.setLastHealthCheckAt(checkAt);
        channel.setLastHealthStatus(ChannelHealthStatus.DEGRADED);
        channel.setLastHealthSource(ChannelHealthSource.CARD);

        // 行为：保存
        Channel persisted = channelGateway.save(channel);

        // 断言：能从 Gateway 读回相同的 3 个字段
        Channel reloaded = channelGateway.findById(persisted.getId()).orElseThrow();
        assertThat(reloaded.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.DEGRADED);
        assertThat(reloaded.getLastHealthSource()).isEqualTo(ChannelHealthSource.CARD);
        assertThat(reloaded.getLastHealthCheckAt()).isEqualTo(checkAt);
    }

    @Test
    void 健康字段未赋值时应保持_null() {
        Provider provider = new Provider();
        provider.setCode("test-null-provider-" + System.nanoTime());
        provider.setName("测试供应商");
        Provider savedProvider = providerGateway.save(provider);

        Channel channel = new Channel();
        channel.setProviderId(savedProvider.getId());
        channel.setName("null-health-channel");
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.PENDING);

        Channel persisted = channelGateway.save(channel);
        Channel reloaded = channelGateway.findById(persisted.getId()).orElseThrow();

        assertThat(reloaded.getLastHealthCheckAt()).isNull();
        assertThat(reloaded.getLastHealthStatus()).isNull();
        assertThat(reloaded.getLastHealthSource()).isNull();
    }
}
