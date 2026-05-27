package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelDiscoveryService;
import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.iam.valueobject.Identity;
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
     * <p>从 request attribute 中获取已认证的 Identity，提取 credentialId（API Key ID）
     * 调用服务查询可见模型。</p>
     */
    @GetMapping
    public ModelDiscoveryResponse listModels(HttpServletRequest request) {
        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null || identity.credentialId() == null) {
            throw new GatewayRequestException("AUTH_REQUIRED", "缺少认证信息");
        }
        return modelDiscoveryService.getVisibleModels(identity.credentialId());
    }
}