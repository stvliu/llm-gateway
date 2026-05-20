package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterRegistry;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider 健康状态追踪器
 *
 * <p>混合探测策略：启动时主动探测，运行中被动推断，超时后重新探测。</p>
 */
@Slf4j
@Component
@EnableConfigurationProperties(ProviderHealthProperties.class)
public class ProviderHealthTracker {

    private final AdapterRegistry adapterRegistry;
    private final ProviderHealthProperties properties;
    private final ConcurrentHashMap<String, ProviderHealthState> states = new ConcurrentHashMap<>();

    public ProviderHealthTracker(AdapterRegistry adapterRegistry, ProviderHealthProperties properties) {
        this.adapterRegistry = adapterRegistry;
        this.properties = properties;
    }

    /**
     * 获取指定 Provider 的健康状态
     *
     * <p>如果状态过期，触发重新探测（同步，保证返回最新状态）。</p>
     */
    public ProviderHealthState getStatus(String providerCode) {
        ProviderHealthState state = states.computeIfAbsent(providerCode, ProviderHealthState::initial);

        if (state.isStale(properties.getStaleThreshold())) {
            triggerProbe(providerCode);
            return states.getOrDefault(providerCode, ProviderHealthState.initial(providerCode));
        }

        return state;
    }

    /**
     * 获取指定 Provider 的缓存状态（不触发探测）
     */
    public ProviderHealthState getCachedStatus(String providerCode) {
        return states.getOrDefault(providerCode, ProviderHealthState.initial(providerCode));
    }

    /**
     * 获取所有 Provider 的缓存状态（不触发探测）
     */
    public List<ProviderHealthState> getAllStatuses() {
        return adapterRegistry.getAllAdapters().stream()
                .map(adapter -> getCachedStatus(adapter.getProviderCode()))
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
                    return updated.consecutiveSuccesses() >= properties.getSuccessThreshold()
                            ? updated
                            : new ProviderHealthState(code, Status.DOWN, updated.lastCheckTime(),
                            updated.lastRequestTime(), 0, updated.consecutiveSuccesses(), null);
                }
                return updated;
            } else {
                ProviderHealthState updated = current.withFailure(error);
                if (updated.consecutiveFailures() >= properties.getFailureThreshold()) {
                    return new ProviderHealthState(code, Status.DOWN, updated.lastCheckTime(),
                            updated.lastRequestTime(), updated.consecutiveFailures(), 0, error);
                }
                return updated;
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

    /**
     * 触发主动探测
     */
    private void triggerProbe(String providerCode) {
        adapterRegistry.getAdapter(providerCode).ifPresent(adapter -> {
            try {
                boolean healthy = adapter.checkConnection();
                Status probeStatus = healthy ? Status.UP : Status.DOWN;
                states.compute(providerCode, (code, existing) ->
                        existing != null ? existing.withProbe(probeStatus) : ProviderHealthState.initial(code).withProbe(probeStatus));
                log.debug("Provider {} probe result: {}", providerCode, probeStatus);
            } catch (Exception e) {
                log.warn("Provider {} probe failed: {}", providerCode, e.getMessage());
                states.compute(providerCode, (code, existing) ->
                        existing != null ? existing.withProbe(Status.DOWN) : ProviderHealthState.initial(code).withProbe(Status.DOWN));
            }
        });
    }
}
