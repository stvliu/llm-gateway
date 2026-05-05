package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent.ConfigType;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent.ChangeType;
import com.codingas.gateway.infrastructure.config.ConfigCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * ConfigChangedEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigChangedEventListener 测试")
class ConfigChangedEventListenerTest {

    @Mock
    private ConfigCacheService cacheService;

    private ConfigChangedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConfigChangedEventListener(cacheService);
    }

    @Nested
    @DisplayName("本地事件监听测试")
    class LocalEventTests {

        @Test
        @DisplayName("PROVIDER 配置变更刷新提供商缓存")
        void onLocalEvent_providerConfig_refreshesProviders() {
            // Given
            ConfigChangedEvent event = new ConfigChangedEvent(ConfigType.PROVIDER, ChangeType.UPDATED, 1L);

            // When
            listener.onLocalEvent(event);

            // Then
            verify(cacheService).refreshProviders();
            verify(cacheService, never()).refreshModels();
            verify(cacheService, never()).refreshApiKeys();
        }

        @Test
        @DisplayName("MODEL 配置变更刷新模型缓存")
        void onLocalEvent_modelConfig_refreshesModels() {
            // Given
            ConfigChangedEvent event = new ConfigChangedEvent(ConfigType.MODEL, ChangeType.UPDATED, 2L);

            // When
            listener.onLocalEvent(event);

            // Then
            verify(cacheService).refreshModels();
            verify(cacheService, never()).refreshProviders();
            verify(cacheService, never()).refreshApiKeys();
        }

        @Test
        @DisplayName("PROVIDER_API_KEY 配置变更刷新 API Key 缓存")
        void onLocalEvent_apiKeyConfig_refreshesApiKeys() {
            // Given
            ConfigChangedEvent event = new ConfigChangedEvent(ConfigType.PROVIDER_API_KEY, ChangeType.UPDATED, 3L);

            // When
            listener.onLocalEvent(event);

            // Then
            verify(cacheService).refreshApiKeys();
            verify(cacheService, never()).refreshProviders();
            verify(cacheService, never()).refreshModels();
        }

        @Test
        @DisplayName("CREATED 变更类型触发刷新")
        void onLocalEvent_createdType_refreshesCache() {
            // Given
            ConfigChangedEvent event = new ConfigChangedEvent(ConfigType.PROVIDER, ChangeType.CREATED, 4L);

            // When
            listener.onLocalEvent(event);

            // Then
            verify(cacheService).refreshProviders();
        }

        @Test
        @DisplayName("DELETED 变更类型触发刷新")
        void onLocalEvent_deletedType_refreshesCache() {
            // Given
            ConfigChangedEvent event = new ConfigChangedEvent(ConfigType.MODEL, ChangeType.DELETED, 5L);

            // When
            listener.onLocalEvent(event);

            // Then
            verify(cacheService).refreshModels();
        }
    }
}
