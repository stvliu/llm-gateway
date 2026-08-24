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
package com.codingas.gateway.usage.tokenlimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenLimit 实体单元测试
 */
@DisplayName("TokenLimit 实体测试")
class TokenLimitTest {

    @Test
    @DisplayName("已用量达到或超过上限判定为超限")
    void isExceeded_usedEqualsOrAboveMax_returnsTrue() {
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setMaxTokens(BigDecimal.valueOf(100));
        tokenLimit.setUsedTokens(BigDecimal.valueOf(100));

        assertThat(tokenLimit.isExceeded()).isTrue();
    }

    @Test
    @DisplayName("已用量低于上限判定为未超限")
    void isExceeded_usedBelowMax_returnsFalse() {
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setMaxTokens(BigDecimal.valueOf(100));
        tokenLimit.setUsedTokens(BigDecimal.valueOf(99));

        assertThat(tokenLimit.isExceeded()).isFalse();
    }
}
