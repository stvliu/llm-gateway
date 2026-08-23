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

import com.codingas.gateway.provider.service.ModelDiscoveryService;
import com.codingas.gateway.provider.model.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户面模型发现控制器
 *
 * <p>兼容 OpenAI /v1/models 格式，供 API Key 持有者查询可用模型。</p>
 */
@RestController
@RequestMapping("/v1/models")
@RequiredArgsConstructor
public class ModelDiscoveryController {

    private final ModelDiscoveryService modelDiscoveryService;

    /**
     * 获取可见模型列表
     *
     * <p>从 request attribute 中获取已认证的 Identity，提取应用 ID（数据面权限锚点）
     * 调用服务查询该应用授权渠道下的可见模型。</p>
     */
    @GetMapping
    public ModelDiscoveryResponse listModels(HttpServletRequest request) {
        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null || identity.applicationId() == null) {
            throw new GatewayRequestException("AUTH_REQUIRED", "缺少认证信息");
        }
        return modelDiscoveryService.getVisibleModels(identity.applicationId());
    }
}