package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模型降级 Invoker
 *
 * <p>包装 KeyFailoverInvoker，捕获 ProviderException 后走降级。
 * 降级后重新路由 + 递归调用。</p>
 */
@Component
public class DegradationInvoker {

    private static final Logger log = LoggerFactory.getLogger(DegradationInvoker.class);

    private final KeyFailoverInvoker keyFailoverInvoker;
    private final DegradationService degradationService;
    private final RoutingResolver routingResolver;

    public DegradationInvoker(KeyFailoverInvoker keyFailoverInvoker,
                               DegradationService degradationService,
                               RoutingResolver routingResolver) {
        this.keyFailoverInvoker = keyFailoverInvoker;
        this.degradationService = degradationService;
        this.routingResolver = routingResolver;
    }

    /**
     * 非流式调用 — 带降级保护
     */
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request,
                                    Protocol inboundProtocol, Long userId, String role,
                                    RoutingStrategy strategy) {
        try {
            return keyFailoverInvoker.invoke(ctx, request);
        } catch (ProviderException e) {
            String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
            if (fallbackModel != null) {
                log.info("模型 {} 降级为 {}，重新调度", request.getModel(), fallbackModel);
                request.setModel(fallbackModel);
                RoutingContext newCtx = routingResolver.resolve(
                        fallbackModel, inboundProtocol, userId, role, strategy);
                return invoke(newCtx, request, inboundProtocol, userId, role, strategy);
            }
            throw e;
        }
    }

    /**
     * 流式调用 — 带降级保护
     */
    public void invokeStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback,
                              Protocol inboundProtocol, Long userId, String role,
                              RoutingStrategy strategy) {
        try {
            keyFailoverInvoker.invokeStream(ctx, request, callback);
        } catch (ProviderException e) {
            String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
            if (fallbackModel != null) {
                log.info("流式调用：模型 {} 降级为 {}，重新调度", request.getModel(), fallbackModel);
                request.setModel(fallbackModel);
                RoutingContext newCtx = routingResolver.resolve(
                        fallbackModel, inboundProtocol, userId, role, strategy);
                invokeStream(newCtx, request, callback, inboundProtocol, userId, role, strategy);
                return;
            }
            throw e;
        }
    }
}
