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

/**
 * 限流超限异常
 *
 * <p>表示请求超过了允许的速率限制。</p>
 */
public class RateLimitExceededException extends ThreatException {

    private static final String CODE = "RATE_LIMIT_EXCEEDED";

    public RateLimitExceededException(String message) {
        super(CODE, message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public RateLimitExceededException() {
        super(CODE, "Rate limit exceeded");
    }
}
