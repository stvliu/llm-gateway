package com.codingas.gateway.domain.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenBucketStatus 单元测试
 */
@DisplayName("TokenBucketStatus 测试")
class TokenBucketStatusTest {

    @Test
    @DisplayName("创建状态成功")
    void create_validParams_success() {
        // when
        TokenBucketStatus status = new TokenBucketStatus(80, 100, 10);

        // then
        assertThat(status.currentTokens()).isEqualTo(80);
        assertThat(status.capacity()).isEqualTo(100);
        assertThat(status.refillRate()).isEqualTo(10);
    }

    @Test
    @DisplayName("计算使用百分比 - 部分使用")
    void usagePercent_partialUse_correctPercent() {
        // given
        TokenBucketStatus status = new TokenBucketStatus(80, 100, 10);

        // when
        double percent = status.usagePercent();

        // then
        assertThat(percent).isEqualTo(20.0);
    }

    @Test
    @DisplayName("计算使用百分比 - 满桶")
    void usagePercent_fullBucket_zeroPercent() {
        // given
        TokenBucketStatus status = new TokenBucketStatus(100, 100, 10);

        // when
        double percent = status.usagePercent();

        // then
        assertThat(percent).isEqualTo(0.0);
    }

    @Test
    @DisplayName("计算使用百分比 - 空桶")
    void usagePercent_emptyBucket_fullPercent() {
        // given
        TokenBucketStatus status = new TokenBucketStatus(0, 100, 10);

        // when
        double percent = status.usagePercent();

        // then
        assertThat(percent).isEqualTo(100.0);
    }
}
