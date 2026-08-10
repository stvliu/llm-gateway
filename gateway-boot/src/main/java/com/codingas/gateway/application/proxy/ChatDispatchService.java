/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.iam.valueobject.Identity;

/**
 * 聊天调度服务
 *
 * <p>七阶段调用链编排：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 */
public interface ChatDispatchService {

    /**
     * 非流式调度
     *
     * @param request 协议请求
     * @param identity 认证结果
     * @param strategy 路由策略
     * @return 协议响应
     */
    ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy);

    /**
     * 流式调度
     *
     * @param request 协议请求
     * @param identity 认证结果
     * @param strategy 路由策略
     * @param callback 流式回调
     */
    void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                        StreamCallback callback);
}