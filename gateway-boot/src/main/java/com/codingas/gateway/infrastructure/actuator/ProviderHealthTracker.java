package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider 健康状态追踪器
 *
 * <p>被动推断策略：基于实际请求结果判断 Provider 健康状态。</p>
 * <p>连续失败 ≥ failureThreshold → DOWN；DOWN 状态下连续成功 ≥ successThreshold → UP。</p>
 */
@Slf4j
@Component
@EnableConfigurationProperties(ProviderHealthProperties.class)
public class ProviderHealthTracker {

    private final ProtocolGatewayRegistry protocolGatewayRegistry;
    private final ProviderHealthProperties properties;
    private final ConcurrentHashMap<String, ProviderHealthState> states = new ConcurrentHashMap<>();

    public ProviderHealthTracker(ProtocolGatewayRegistry protocolGatewayRegistry, ProviderHealthProperties properties) {
        this.protocolGatewayRegistry = protocolGatewayRegistry;
        this.properties = properties;
    }

    /**
     * 获取指定 Provider 的健康状态
     *
     * <p>如果状态过期，标记为需要重新评估（由下次实际请求结果驱动）。</p>
     */
    public ProviderHealthState getStatus(String providerCode) {
        ProviderHealthState state = states.computeIfAbsent(providerCode, ProviderHealthState::initial);

        if (state.isStale(properties.getStaleThreshold())) {
            log.debug("Provider {} 状态过期，等待下次请求结果重新评估", providerCode);
        }

        return state;
    }

    /**
     * 获取指定 Provider 的缓存状态（不触发重新评估）
     */
    public ProviderHealthState getCachedStatus(String providerCode) {
        return states.getOrDefault(providerCode, ProviderHealthState.initial(providerCode));
    }

    /**
     * 获取所有 Provider 的缓存状态（不触发重新评估）
     */
    public List<ProviderHealthState> getAllStatuses() {
        return protocolGatewayRegistry.getAllGateways().stream()
                .map(gateway -> getCachedStatus(gateway.getProtocolName()))
                .toList();
    }

    /**
     * 记录实际请求结果（被动推断）
     *
     * <p>根据连续失败/成功次数和配置阈值决定状态转换：</p>
     * <ul>
     *   <li>连续失败 ≥ failureThreshold → DOWN</li>
     *   <li>DOWN 状态下连续成功 ≥ successThreshold → UP</li>
     *   <li>非 DOWN 状态下单次成功 → UP</li>
     * </ul>
     */
    public void recordRequestResult(String providerCode, boolean success, String error) {
        states.compute(providerCode, (code, existing) -> {
            ProviderHealthState current = existing != null ? existing : ProviderHealthState.initial(code);

            if (success) {
                ProviderHealthState updated = current.withSuccess();
                if (current.status() == Status.DOWN) {
                    // DOWN 状态下连续成功未达阈值，保持 DOWN
                    return updated.consecutiveSuccesses() >= properties.getSuccessThreshold()
                            ? updated
                            : new ProviderHealthState(code, Status.DOWN, updated.lastRequestTime(),
                            0, updated.consecutiveSuccesses(), null);
                }
                return updated;
            } else {
                ProviderHealthState updated = current.withFailure(error);
                if (updated.consecutiveFailures() >= properties.getFailureThreshold()) {
                    return updated; // withFailure 已经设置状态为 DOWN
                }
                // 未达阈值，保持当前状态但记录失败
                return new ProviderHealthState(code, current.status(), updated.lastRequestTime(),
                        updated.consecutiveFailures(), 0, error);
            }
        });
    }

    /**
     * 是否至少有一个健康的 Provider
     */
    public boolean hasHealthyProvider() {
        return getAllStatuses().stream()
                .anyMatch(state -> state.status() == Status.UP);
    }
}