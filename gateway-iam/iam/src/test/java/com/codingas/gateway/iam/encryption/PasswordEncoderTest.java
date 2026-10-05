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
package com.codingas.gateway.iam.encryption;

import cn.dev33.satoken.secure.SaSecureUtil;
import com.codingas.gateway.common.exception.GatewayRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PasswordEncoder} 单元测试。
 *
 * <p>覆盖：BCrypt 编码格式与加盐、双格式校验（BCrypt + 存量无盐 SHA-256）、
 * 非法格式容错、存量格式升级判定。</p>
 */
@DisplayName("PasswordEncoder 密码哈希（BCrypt + 存量 SHA-256 兼容）")
class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("encode：产生 BCrypt 格式哈希（$2 前缀、60 字符）")
    void encode_producesBcryptHash() {
        String hash = encoder.encode("password123");

        assertThat(hash).startsWith("$2");
        assertThat(hash).hasSize(60);
    }

    @Test
    @DisplayName("encode：同密码两次编码产生不同盐（哈希不同但均可校验）")
    void encode_saltsEachInvocation() {
        String first = encoder.encode("password123");
        String second = encoder.encode("password123");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("password123", first)).isTrue();
        assertThat(encoder.matches("password123", second)).isTrue();
    }

    @Test
    @DisplayName("matches：BCrypt 格式哈希正确校验（正确通过 / 错误拒绝）")
    void matches_bcryptHash() {
        String hash = encoder.encode("password123");

        assertThat(encoder.matches("password123", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }

    @Test
    @DisplayName("matches：兼容存量无盐 SHA-256 哈希（64 位 hex），透明升级期不得拒绝老用户")
    void matches_legacySha256Hash() {
        String legacyHash = SaSecureUtil.sha256("password123");

        assertThat(encoder.matches("password123", legacyHash)).isTrue();
        assertThat(encoder.matches("wrong-password", legacyHash)).isFalse();
    }

    @Test
    @DisplayName("matches：非法格式哈希返回 false 而非抛异常")
    void matches_malformedHashReturnsFalse() {
        assertThat(encoder.matches("password123", "not-a-hash")).isFalse();
        assertThat(encoder.matches("password123", "")).isFalse();
    }

    @Test
    @DisplayName("needsUpgrade：存量 SHA-256 hex 判定 true，BCrypt 格式判定 false")
    void needsUpgrade_detectsLegacyFormat() {
        String legacyHash = SaSecureUtil.sha256("password123");

        assertThat(encoder.needsUpgrade(legacyHash)).isTrue();
        assertThat(encoder.needsUpgrade(encoder.encode("password123"))).isFalse();
    }

    @Test
    @DisplayName("needsUpgrade：非法格式视为需升级（容错，不抛异常）")
    void needsUpgrade_malformedTreatedAsUpgrade() {
        assertThat(encoder.needsUpgrade("not-a-hash")).isTrue();
        assertThat(encoder.needsUpgrade("")).isTrue();
    }

    @Test
    @DisplayName("encode：超过 72 字节上限抛 GatewayRequestException（中文提示，映射 4xx）")
    void encode_overByteLimit_throwsDomainException() {
        // 30 个汉字 = UTF-8 90 字节，超过 BCrypt 72 字节上限
        String tooLong = "字".repeat(30);

        assertThatThrownBy(() -> encoder.encode(tooLong))
                .isInstanceOf(GatewayRequestException.class)
                .hasMessageContaining("72");

        // 边界：24 个汉字 = 72 字节，恰好不抛
        assertThat(encoder.encode("字".repeat(24))).startsWith("$2");
    }
}
