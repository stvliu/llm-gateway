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
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 渠道不存在异常
 */
public class ChannelNotFoundException extends GatewayException {

    public ChannelNotFoundException(Long channelId) {
        super("CHANNEL_NOT_FOUND", "渠道不存在: " + channelId);
    }
}
