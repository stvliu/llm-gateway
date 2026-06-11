package com.codingas.gateway.application.degradation;

import com.codingas.gateway.application.degradation.DegradationProperties.DegradationChain;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.infrastructure.actuator.ProviderHealthTracker;
import com.codingas.gateway.infrastructure.actuator.ProviderHealthState;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 智能降级服务实现
 *
 * <p>基于降级链配置，在主模型不可用时自动切换到备选模型。
 * 定期健康检查，恢复后标记原模型可用。</p>
 */
@Service
@EnableConfigurationProperties(DegradationProperties.class)
public class DegradationServiceImpl implements DegradationService {

    private static final Logger log = LoggerFactory.getLogger(DegradationServiceImpl.class);

    private final DegradationProperties properties;
    private final ProviderHealthTracker healthTracker;
    private final MeterRegistry meterRegistry;
    private final DomainEventPublisher eventPublisher;

    /** 已降级的模型 → 降级状态 */
    private final ConcurrentMap<String, DegradedModel> degradedModels = new ConcurrentHashMap<>();

    /** 主模型 → 降级链 查找索引 */
    private final Map<String, DegradationChain> chainIndex;

    public DegradationServiceImpl(DegradationProperties properties,
                                   ProviderHealthTracker healthTracker,
                                   MeterRegistry meterRegistry,
                                   DomainEventPublisher eventPublisher) {
        this.properties = properties;
        this.healthTracker = healthTracker;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.chainIndex = properties.getChains().stream()
                .collect(Collectors.toMap(DegradationChain::getPrimary, c -> c));
        validateChains();
        log.info("智能降级已初始化，{} 条降级链", chainIndex.size());
    }

    /**
     * 校验降级链配置：检查循环引用和空链
     */
    private void validateChains() {
        for (DegradationChain chain : properties.getChains()) {
            if (chain.getFallbacks().isEmpty()) {
                log.warn("降级链 {} 没有备选模型", chain.getPrimary());
            }
            if (chain.getFallbacks().contains(chain.getPrimary())) {
                throw new IllegalArgumentException(
                        "降级链循环引用: " + chain.getPrimary() + " 的备选包含自身");
            }
            // 检查备选链中是否存在循环
            List<String> allModels = chain.getFallbacks();
            for (int i = 0; i < allModels.size(); i++) {
                String fb = allModels.get(i);
                DegradationChain subChain = chainIndex.get(fb);
                if (subChain != null && subChain.getFallbacks().contains(chain.getPrimary())) {
                    throw new IllegalArgumentException(
                            "降级链循环引用: " + chain.getPrimary() + " ↔ " + fb);
                }
            }
        }
    }

    @Override
    public String degrade(String originalModel, ProviderErrorType reason) {
        if (!properties.isEnabled()) {
            return null;
        }

        DegradationChain chain = chainIndex.get(originalModel);
        if (chain == null) {
            return null;
        }

        for (int i = 0; i < chain.getFallbacks().size() && i < properties.getMaxChainDepth(); i++) {
            String fallback = chain.getFallbacks().get(i);
            if (isAvailable(fallback)) {
                degradedModels.put(originalModel, new DegradedModel(originalModel, fallback, reason,
                        i + 1, chain.getRecovery().getSuccessThreshold()));

                meterRegistry.counter("gateway.degradation.triggered",
                        "from_model", originalModel,
                        "to_model", fallback,
                        "reason", reason.name()).increment();

                eventPublisher.publish(new DegradationEvent(
                        null, null, originalModel, fallback, reason, i + 1, Instant.now()));

                log.info("模型 {} 降级 → {} (原因: {}, 步骤: {})", originalModel, fallback, reason, i + 1);
                return fallback;
            }
        }

        log.warn("模型 {} 降级失败: 所有备选均不可用", originalModel);
        meterRegistry.counter("gateway.degradation.exhausted",
                "model", originalModel,
                "reason", reason.name()).increment();
        throw new ProviderException(reason, "ALL_MODELS_DEGRADED: 模型 " + originalModel + " 所有备选均不可用");
    }

    @Override
    public boolean canRecover(String model) {
        DegradedModel degraded = degradedModels.get(model);
        return degraded == null || degraded.isRecovered();
    }

    /**
     * 判断备选模型是否可用
     */
    private boolean isAvailable(String model) {
        // 检查是否已被降级到其他模型
        DegradedModel degraded = degradedModels.get(model);
        if (degraded != null && !degraded.isRecovered()) {
            return false;
        }
        // 检查 Provider 健康状态
        ProviderHealthState state = healthTracker.getCachedStatus(model);
        return state.status() != Status.DOWN;
    }

    @Scheduled(fixedDelayString = "${gateway.degradation.check-interval:60000}")
    @Override
    public void recoveryCheck() {
        if (degradedModels.isEmpty()) {
            return;
        }

        log.debug("执行降级恢复检查，{} 个模型处于降级状态", degradedModels.size());

        for (Map.Entry<String, DegradedModel> entry : degradedModels.entrySet()) {
            String model = entry.getKey();
            DegradedModel degraded = entry.getValue();

            if (degraded.isRecovered()) {
                continue;
            }

            ProviderHealthState state = healthTracker.getCachedStatus(model);
            if (state.status() == Status.UP) {
                degraded.recordSuccess();
                if (degraded.consecutiveSuccesses() >= degraded.recoveryThreshold()) {
                    degraded.markRecovered();
                    meterRegistry.counter("gateway.degradation.recovered",
                            "model", model).increment();
                    eventPublisher.publish(new DegradationRecoveredEvent(model, Instant.now()));
                    log.info("模型 {} 已恢复，降级自动回切", model);
                }
            } else {
                degraded.resetSuccesses();
            }
        }
    }

    /**
     * 降级中的模型状态记录
     */
    static class DegradedModel {
        private final String originalModel;
        private final String currentModel;
        private final ProviderErrorType reason;
        private final int chainStep;
        private final int recoveryThreshold;
        private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
        private volatile boolean recovered = false;

        DegradedModel(String originalModel, String currentModel, ProviderErrorType reason,
                      int chainStep, int recoveryThreshold) {
            this.originalModel = originalModel;
            this.currentModel = currentModel;
            this.reason = reason;
            this.chainStep = chainStep;
            this.recoveryThreshold = recoveryThreshold;
        }

        void recordSuccess() {
            consecutiveSuccesses.incrementAndGet();
        }

        void resetSuccesses() {
            consecutiveSuccesses.set(0);
        }

        void markRecovered() {
            this.recovered = true;
        }

        boolean isRecovered() { return recovered; }
        int consecutiveSuccesses() { return consecutiveSuccesses.get(); }
        int recoveryThreshold() { return recoveryThreshold; }
        String originalModel() { return originalModel; }
        String currentModel() { return currentModel; }
        ProviderErrorType reason() { return reason; }
        int chainStep() { return chainStep; }
    }
}
