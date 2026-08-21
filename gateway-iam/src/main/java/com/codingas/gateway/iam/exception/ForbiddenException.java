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
package com.codingas.gateway.iam.exception;

/**
 * 无权限异常
 *
 * <p>表示用户已认证但无权访问资源。</p>
 */
public class ForbiddenException extends IamException {

    private static final String CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(CODE, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public ForbiddenException() {
        super(CODE, "Access denied");
    }
}
