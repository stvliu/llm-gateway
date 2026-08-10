/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.protocol.openai;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.tuning.ProtocolTuner;
import org.springframework.stereotype.Component;

/**
 * OpenAI 协议出站调谐器
 *
 * <p>补全 OpenAI 请求的默认值：</p>
 * <ul>
 *   <li>max_tokens 缺省补 4096</li>
 * </ul>
 */
@Component
public class OpenAITuner implements ProtocolTuner<OpenAIChatRequest> {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    @Override
    public String getProtocol() {
        return "openai";
    }

    @Override
    public OpenAIChatRequest tune(OpenAIChatRequest request) {
        // 补全 max_tokens 默认值
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(DEFAULT_MAX_TOKENS);
        }
        return request;
    }
}