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

import com.codingas.gateway.protocol.ProtocolRequest;

/**
 * 协议校验器接口
 *
 * @param <T> 协议请求类型
 */
public interface ProtocolValidator<T extends ProtocolRequest> {

    /**
     * 获取支持的协议标识
     */
    String getProtocol();

    /**
     * 入站校验协议请求
     */
    void validate(T request);
}
