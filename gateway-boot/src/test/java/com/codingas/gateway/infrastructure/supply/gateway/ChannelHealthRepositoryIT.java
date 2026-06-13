package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
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
@SpringBootTest
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
        channel.setState(Channel.State.PENDING);
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
        channel.setState(Channel.State.PENDING);

        Channel persisted = channelGateway.save(channel);
        Channel reloaded = channelGateway.findById(persisted.getId()).orElseThrow();

        assertThat(reloaded.getLastHealthCheckAt()).isNull();
        assertThat(reloaded.getLastHealthStatus()).isNull();
        assertThat(reloaded.getLastHealthSource()).isNull();
    }
}
