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
package com.codingas.gateway.application.proxy.failover;

import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorClassifier 单元测试
 *
 * <p>验证错误分流表（D3）映射：按 ProviderErrorType 映射到 FailoverDecision。</p>
 * <ul>
 *   <li>INVALID_REQUEST → NONE（请求级错误，换哪都无效）</li>
 *   <li>共因故障（AUTH/QUOTA/RATE_LIMIT/NETWORK/UPSTREAM_ERROR/TIMEOUT/SERVER_ERROR）→ L1（换渠道）</li>
 *   <li>UNKNOWN_ERROR → NONE（L2 模型降级层已删除，未知错误不再触发换模型，归为不转移）</li>
 *   <li>null → NONE（编程错误，不转移直接抛出原异常）</li>
 * </ul>
 */
@DisplayName("ErrorClassifier 错误分流表测试")
class ErrorClassifierTest {

    private final ErrorClassifier errorClassifier = new ErrorClassifier();

    @Nested
    @DisplayName("请求级错误 → NONE（换哪都无效）")
    class RequestLevelTests {

        @Test
        @DisplayName("INVALID_REQUEST → NONE")
        void classify_invalidRequest_returnsNone() {
            // when
            FailoverDecision decision = errorClassifier.classify(ProviderErrorType.INVALID_REQUEST);
            // then
            assertThat(decision).isEqualTo(FailoverDecision.NONE);
        }
    }

    @Nested
    @DisplayName("共因故障 → L1（换渠道）")
    class CommonCauseTests {

        @ParameterizedTest(name = "{0} → L1")
        @EnumSource(value = ProviderErrorType.class, names = {
                "AUTHENTICATION_ERROR",
                "RATE_LIMIT_ERROR",
                "QUOTA_EXCEEDED",
                "TIMEOUT_ERROR",
                "UPSTREAM_ERROR",
                "SERVICE_UNAVAILABLE",
                "NETWORK_ERROR"
        })
        void classify_commonCause_returnsL1(ProviderErrorType type) {
            // when
            FailoverDecision decision = errorClassifier.classify(type);
            // then
            assertThat(decision).isEqualTo(FailoverDecision.L1);
        }
    }

    @Nested
    @DisplayName("未知错误 → NONE（L2 降级层已删除，不再换模型）")
    class ModelCapabilityTests {

        @Test
        @DisplayName("UNKNOWN_ERROR → NONE")
        void classify_unknownError_returnsNone() {
            // when
            FailoverDecision decision = errorClassifier.classify(ProviderErrorType.UNKNOWN_ERROR);
            // then
            assertThat(decision).isEqualTo(FailoverDecision.NONE);
        }
    }

    @Nested
    @DisplayName("null 输入处理")
    class NullInputTests {

        @ParameterizedTest(name = "null → NONE")
        @NullSource
        void classify_null_returnsNone(ProviderErrorType type) {
            // when
            FailoverDecision decision = errorClassifier.classify(type);
            // then
            assertThat(decision).isEqualTo(FailoverDecision.NONE);
        }
    }
}
