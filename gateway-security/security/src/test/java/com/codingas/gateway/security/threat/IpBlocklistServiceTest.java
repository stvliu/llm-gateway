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

import com.codingas.gateway.security.threat.IpBlocklistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * IpBlocklistManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpBlocklistManager")
class IpBlocklistManagerTest {

    @Mock
    private IpBlocklistRepository ipBlockRepository;

    @InjectMocks
    private IpBlocklistManager ipBlocklistManager;

    @Test
    @DisplayName("isBlocked null IP 应返回 false")
    void isBlocked_nullIp_returnsFalse() {
        boolean result = ipBlocklistManager.isBlocked(null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlocked blank IP 应返回 false")
    void isBlocked_blankIp_returnsFalse() {
        boolean result = ipBlocklistManager.isBlocked("");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlocked 在黑名单中的 IP 应返回 true")
    void isBlocked_blockedIp_returnsTrue() {
        when(ipBlockRepository.isBlocked("192.168.1.100")).thenReturn(true);

        boolean result = ipBlocklistManager.isBlocked("192.168.1.100");

        assertThat(result).isTrue();
    }
}
