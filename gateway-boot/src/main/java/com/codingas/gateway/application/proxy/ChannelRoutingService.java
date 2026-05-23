package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.iam.service.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 通道路由服务
 * <p>
 * 基于 UserApiKey 关联的多产品匹配路由。
 */
@Service
public class ChannelRoutingService {

    private static final Logger log = LoggerFactory.getLogger(ChannelRoutingService.class);

    private final ProductRoutingService productRoutingService;

    public ChannelRoutingService(ProductRoutingService productRoutingService) {
        this.productRoutingService = productRoutingService;
    }

    /**
     * 解析路由上下文
     *
     * @param authResult 认证结果
     * @param model      请求的模型名
     * @param protocol   请求协议
     * @return 路由上下文
     */
    public RoutingContext resolve(Identity authResult, String model, String protocol) {
        log.debug("Routing: credentialId={}, model={}, protocol={}",
                authResult.credentialId(), model, protocol);

        return productRoutingService.resolve(authResult.credentialId(), model, protocol);
    }
}