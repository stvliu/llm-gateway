package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channel 实体健康状态字段读写与枚举值的单元测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>三个健康字段的 Getter/Setter 透传</li>
 *   <li>未赋值时默认为 null</li>
 *   <li>{@link ChannelHealthStatus} 枚举值集合（HEALTHY / DEGRADED / FAILED / UNKNOWN）</li>
 *   <li>{@link ChannelHealthSource} 枚举值集合（CARD / DRAWER / PRECHECK）</li>
 * </ul>
 */
class ChannelHealthFieldsTest {

    @Test
    void 应能读写三个健康字段() {
        Channel channel = new Channel();
        Instant now = Instant.now();
        channel.setLastHealthCheckAt(now);
        channel.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        channel.setLastHealthSource(ChannelHealthSource.DRAWER);

        assertThat(channel.getLastHealthCheckAt()).isEqualTo(now);
        assertThat(channel.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(channel.getLastHealthSource()).isEqualTo(ChannelHealthSource.DRAWER);
    }

    @Test
    void 默认值为_null() {
        Channel channel = new Channel();
        assertThat(channel.getLastHealthCheckAt()).isNull();
        assertThat(channel.getLastHealthStatus()).isNull();
        assertThat(channel.getLastHealthSource()).isNull();
    }

    @Test
    void 健康状态枚举包含四个值() {
        assertThat(ChannelHealthStatus.values())
            .containsExactlyInAnyOrder(
                ChannelHealthStatus.HEALTHY,
                ChannelHealthStatus.DEGRADED,
                ChannelHealthStatus.FAILED,
                ChannelHealthStatus.UNKNOWN);
    }

    @Test
    void 健康来源枚举包含三个值() {
        assertThat(ChannelHealthSource.values())
            .containsExactlyInAnyOrder(
                ChannelHealthSource.CARD,
                ChannelHealthSource.DRAWER,
                ChannelHealthSource.PRECHECK);
    }
}
