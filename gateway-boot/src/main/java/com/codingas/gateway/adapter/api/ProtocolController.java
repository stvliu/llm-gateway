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
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * 协议列表 REST API
 *
 * <p>提供已注册协议的名称列表，供前端动态获取可选协议。</p>
 */
@RestController
@RequestMapping("/api/v1/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final UpstreamClientRegistry upstreamClientRegistry;

    private static final Map<String, String> PROTOCOL_LABELS = Map.of(
            "openai", "OpenAI Chat Completions",
            "anthropic", "Anthropic Messages"
    );

    /**
     * 获取所有已注册协议
     */
    @GetMapping
    public List<Map<String, String>> listProtocols() {
        return upstreamClientRegistry.getSupportedProtocols().stream()
            .map(protocol -> Map.of("name", protocol, "label", PROTOCOL_LABELS.getOrDefault(protocol, protocol)))
            .toList();
    }
}