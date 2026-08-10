/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.protocol.contract;

/**
 * 协议响应接口，所有协议响应 DTO 实现此接口
 */
public interface ProtocolResponse {

    /**
     * 获取模型名称
     */
    String getModel();

    /**
     * 获取结束原因
     */
    String getFinishReason();
}