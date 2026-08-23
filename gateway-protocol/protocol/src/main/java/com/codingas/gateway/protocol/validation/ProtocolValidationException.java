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
package com.codingas.gateway.protocol.validation;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 协议校验异常
 */
public class ProtocolValidationException extends GatewayException {

    private static final String ERROR_CODE = "PROTOCOL_VALIDATION_ERROR";

    private final String protocol;
    private final String field;
    private final String violation;

    public ProtocolValidationException(String protocol, String field, String violation) {
        super(ERROR_CODE, String.format("协议校验失败 [%s]: 字段 '%s' %s", protocol, field, violation));
        this.protocol = protocol;
        this.field = field;
        this.violation = violation;
    }

    public String getProtocol() { return protocol; }
    public String getField() { return field; }
    public String getViolation() { return violation; }
}
