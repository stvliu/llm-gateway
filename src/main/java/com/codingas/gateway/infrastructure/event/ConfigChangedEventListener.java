package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import com.codingas.gateway.infrastructure.config.ConfigCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置变更事件监听器
 *
 * <p>处理本地事件。企业版可扩展支持 Redis Pub/Sub。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigChangedEventListener {

    private final ConfigCacheService cacheService;

    // ========== 本地事件监听 ==========

    @EventListener
    public void onLocalEvent(ConfigChangedEvent event) {
        log.info("Received local config event: {}", event);
        handleEvent(event);
    }

    // ========== 事件处理 ==========

    private void handleEvent(ConfigChangedEvent event) {
        switch (event.getConfigType()) {
            case PROVIDER -> cacheService.refreshProviders();
            case MODEL -> cacheService.refreshModels();
            case PROVIDER_API_KEY -> cacheService.refreshApiKeys();
        }
    }
}
