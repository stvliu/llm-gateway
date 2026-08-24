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
package com.codingas.gateway.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditContext 单元测试
 *
 * <p>覆盖 record 直接实例化（构造器 + 访问器）与 Builder 全量/部分/默认构建。</p>
 */
@DisplayName("AuditContext 测试")
class AuditContextTest {

    @Nested
    @DisplayName("record 直接实例化")
    class DirectInstantiationTests {

        @Test
        @DisplayName("直接构造并读取全部字段")
        void instantiate_allFields_accessible() {
            // given
            AuditContext context = new AuditContext(
                    42L, "API_CALL", "/v1/chat/completions",
                    "POST", "/v1/chat/completions", "{\"model\":\"gpt-4\"}",
                    200, 1234, "trace-abc", "192.168.1.1",
                    "Mozilla/5.0", null);

            // then
            assertThat(context.userId()).isEqualTo(42L);
            assertThat(context.action()).isEqualTo("API_CALL");
            assertThat(context.resource()).isEqualTo("/v1/chat/completions");
            assertThat(context.requestMethod()).isEqualTo("POST");
            assertThat(context.requestPath()).isEqualTo("/v1/chat/completions");
            assertThat(context.requestBody()).isEqualTo("{\"model\":\"gpt-4\"}");
            assertThat(context.responseStatus()).isEqualTo(200);
            assertThat(context.responseTime()).isEqualTo(1234);
            assertThat(context.traceId()).isEqualTo("trace-abc");
            assertThat(context.ipAddress()).isEqualTo("192.168.1.1");
            assertThat(context.userAgent()).isEqualTo("Mozilla/5.0");
            assertThat(context.errorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder 构建")
    class BuilderTests {

        @Test
        @DisplayName("Builder 全字段构建")
        void builder_allFields_buildSuccess() {
            // when
            AuditContext context = AuditContext.builder()
                    .userId(7L)
                    .action("LOGIN")
                    .resource("/api/auth/login")
                    .requestMethod("GET")
                    .requestPath("/api/auth/login")
                    .requestBody("{}")
                    .responseStatus(401)
                    .responseTime(88)
                    .traceId("trace-xyz")
                    .ipAddress("10.0.0.1")
                    .userAgent("curl/8.0")
                    .errorMessage("bad credentials")
                    .build();

            // then
            assertThat(context.userId()).isEqualTo(7L);
            assertThat(context.action()).isEqualTo("LOGIN");
            assertThat(context.resource()).isEqualTo("/api/auth/login");
            assertThat(context.requestMethod()).isEqualTo("GET");
            assertThat(context.requestPath()).isEqualTo("/api/auth/login");
            assertThat(context.requestBody()).isEqualTo("{}");
            assertThat(context.responseStatus()).isEqualTo(401);
            assertThat(context.responseTime()).isEqualTo(88);
            assertThat(context.traceId()).isEqualTo("trace-xyz");
            assertThat(context.ipAddress()).isEqualTo("10.0.0.1");
            assertThat(context.userAgent()).isEqualTo("curl/8.0");
            assertThat(context.errorMessage()).isEqualTo("bad credentials");
        }

        @Test
        @DisplayName("Builder 部分字段构建，未设置字段为 null")
        void builder_partialFields_remainingNull() {
            // when
            AuditContext context = AuditContext.builder()
                    .userId(1L)
                    .action("API_CALL")
                    .build();

            // then
            assertThat(context.userId()).isEqualTo(1L);
            assertThat(context.action()).isEqualTo("API_CALL");
            assertThat(context.resource()).isNull();
            assertThat(context.requestMethod()).isNull();
            assertThat(context.requestPath()).isNull();
            assertThat(context.requestBody()).isNull();
            assertThat(context.responseStatus()).isNull();
            assertThat(context.responseTime()).isNull();
            assertThat(context.traceId()).isNull();
            assertThat(context.ipAddress()).isNull();
            assertThat(context.userAgent()).isNull();
            assertThat(context.errorMessage()).isNull();
        }

        @Test
        @DisplayName("Builder 不设置任何字段，全部为 null")
        void builder_noFields_allNull() {
            // when
            AuditContext context = AuditContext.builder().build();

            // then
            assertThat(context.userId()).isNull();
            assertThat(context.action()).isNull();
            assertThat(context.resource()).isNull();
            assertThat(context.requestMethod()).isNull();
            assertThat(context.requestPath()).isNull();
            assertThat(context.requestBody()).isNull();
            assertThat(context.responseStatus()).isNull();
            assertThat(context.responseTime()).isNull();
            assertThat(context.traceId()).isNull();
            assertThat(context.ipAddress()).isNull();
            assertThat(context.userAgent()).isNull();
            assertThat(context.errorMessage()).isNull();
        }

        @Test
        @DisplayName("Builder 链式调用返回自身，可连续设置")
        void builder_chainable_returnsThis() {
            // given
            AuditContext.Builder builder = AuditContext.builder();

            // when
            AuditContext.Builder same = builder.userId(1L).action("A").resource("R");

            // then
            assertThat(same).isSameAs(builder);
        }
    }
}
