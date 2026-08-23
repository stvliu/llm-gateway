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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.provider.model.ModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 路由器责任链 — 按 @Order 排序依次执行 Router
 *
 * <p>非强制 Router 过滤结果为空时跳过，强制 Router 为空时直接返回空列表。</p>
 */
@Component
public class RouterChain {

    private static final Logger log = LoggerFactory.getLogger(RouterChain.class);

    private final List<Router> routers;

    public RouterChain(List<Router> routers) {
        this.routers = routers.stream()
                .sorted(Comparator.comparingInt(
                        r -> {
                            Order order = r.getClass().getAnnotation(Order.class);
                            return order != null ? order.value() : Integer.MAX_VALUE;
                        }))
                .toList();
        log.info("RouterChain initialized with {} routers: {}", this.routers.size(),
                this.routers.stream().map(r -> r.getClass().getSimpleName()).toList());
    }

    /**
     * 执行路由链过滤
     *
     * @param instances 原始候选实例列表
     * @param request   路由请求上下文
     * @return 最终过滤后的实例列表
     */
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        List<ModelInstance> candidates = instances;

        for (Router router : routers) {
            List<ModelInstance> filtered = router.filter(candidates, request);
            if (filtered.isEmpty()) {
                if (router.isForce()) {
                    log.debug("Router {} returned empty, chain terminated", router.getClass().getSimpleName());
                    return List.of();
                }
                log.debug("Router {} returned empty, skipping (non-force)", router.getClass().getSimpleName());
                continue;
            }
            log.debug("Router {} filtered {} -> {}", router.getClass().getSimpleName(),
                    candidates.size(), filtered.size());
            candidates = filtered;
        }

        return candidates;
    }
}
