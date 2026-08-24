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
package com.codingas.gateway.security.threat;

import com.codingas.gateway.security.dataprotection.SensitiveDataRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 安全域实体状态方法单元测试（IpBlocklist/SensitiveDataRule）
 */
@DisplayName("安全域实体测试")
class SecurityEntityTest {

    @Test
    @DisplayName("IpBlocklist 到期时间未到未过期")
    void ipBlocklist_notExpired_returnsFalse() {
        IpBlocklist blocklist = new IpBlocklist();
        blocklist.setExpiresAt(Instant.now().plusSeconds(60));

        assertThat(blocklist.isExpired()).isFalse();
    }

    @Test
    @DisplayName("IpBlocklist 到期时间已过判定过期")
    void ipBlocklist_pastExpiry_returnsTrue() {
        IpBlocklist blocklist = new IpBlocklist();
        blocklist.setExpiresAt(Instant.now().minusSeconds(60));

        assertThat(blocklist.isExpired()).isTrue();
    }

    @Test
    @DisplayName("IpBlocklist 无到期时间视为未过期")
    void ipBlocklist_noExpiry_returnsFalse() {
        IpBlocklist blocklist = new IpBlocklist();

        assertThat(blocklist.isExpired()).isFalse();
    }

    @Test
    @DisplayName("SensitiveDataRule enabled 为 true 判定启用")
    void sensitiveDataRule_enabled_returnsTrue() {
        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setEnabled(true);

        assertThat(rule.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("SensitiveDataRule enabled 为 false 判定未启用")
    void sensitiveDataRule_disabled_returnsFalse() {
        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setEnabled(false);

        assertThat(rule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("SensitiveDataRule enabled 为 null 判定未启用")
    void sensitiveDataRule_nullEnabled_returnsFalse() {
        SensitiveDataRule rule = new SensitiveDataRule();

        assertThat(rule.isEnabled()).isFalse();
    }
}
