/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.tuning.ProtocolTuner;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 出站调谐编排器
 *
 * <p>在请求发送到上游之前，执行两层调谐：</p>
 * <ol>
 *   <li>协议级调谐：通过 ProtocolTuner 补全协议特定的默认值</li>
 *   <li>渠道级调谐：根据路由上下文替换模型名</li>
 * </ol>
 */
@Component
public class OutboundTuner {

    private static final Logger log = LoggerFactory.getLogger(OutboundTuner.class);

    private final Map<String, ProtocolTuner<?>> tunersByProtocol;

    public OutboundTuner(List<ProtocolTuner<?>> tuners) {
        this.tunersByProtocol = tuners.stream()
                .collect(Collectors.toMap(ProtocolTuner::getProtocol, Function.identity()));
    }

    /**
     * 对请求执行出站调谐
     *
     * @param request 原始请求
     * @param context 路由上下文（包含上游模型名映射）
     * @return 调谐后的请求
     */
    @SuppressWarnings("unchecked")
    public <T extends ProtocolRequest> T tune(T request, RoutingContext context) {
        // 第一层：协议级调谐
        String protocol = request.getProtocol();
        ProtocolTuner<T> tuner = (ProtocolTuner<T>) tunersByProtocol.get(protocol);
        if (tuner != null) {
            request = tuner.tune(request);
            log.debug("协议级调谐完成: protocol={}", protocol);
        }

        // 第二层：渠道级调谐 — 模型名替换
        String upstreamModelName = context.upstreamModelName();
        if (upstreamModelName != null && !upstreamModelName.isBlank()) {
            request.setModel(upstreamModelName);
            log.debug("模型名替换: {} -> {}", context.modelName(), upstreamModelName);
        }

        return request;
    }
}