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

import com.codingas.gateway.security.threat.TokenBucketStatus;
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
        TokenBucketStatus status = new TokenBucketStatus(80, 100, 10);

        assertThat(status.currentTokens()).isEqualTo(80);
        assertThat(status.capacity()).isEqualTo(100);
        assertThat(status.refillRate()).isEqualTo(10);
    }
}
