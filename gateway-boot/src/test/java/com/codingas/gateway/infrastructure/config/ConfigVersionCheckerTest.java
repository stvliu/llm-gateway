package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * ConfigVersionChecker 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigVersionChecker 测试")
class ConfigVersionCheckerTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderApiKeyGateway apiKeyGateway;

    @Mock
    private ConfigCacheService cacheService;

    @InjectMocks
    private ConfigVersionChecker versionChecker;

    @Nested
    @DisplayName("initVersions 方法测试")
    class InitVersionsTests {

        @Test
        @DisplayName("初始化版本号")
        void initVersions_loadsCurrentVersions() {
            // given
            when(providerGateway.getMaxVersion()).thenReturn(10L);
            when(modelGateway.getMaxVersion()).thenReturn(20L);
            when(apiKeyGateway.getMaxVersion()).thenReturn(30L);

            // when
            versionChecker.initVersions();

            // then
            verify(providerGateway).getMaxVersion();
            verify(modelGateway).getMaxVersion();
            verify(apiKeyGateway).getMaxVersion();
        }

        @Test
        @DisplayName("初始版本为 0")
        void initVersions_zeroVersions_handlesGracefully() {
            // given
            when(providerGateway.getMaxVersion()).thenReturn(0L);
            when(modelGateway.getMaxVersion()).thenReturn(0L);
            when(apiKeyGateway.getMaxVersion()).thenReturn(0L);

            // when
            versionChecker.initVersions();

            // then
            verify(providerGateway).getMaxVersion();
        }
    }

    @Nested
    @DisplayName("checkVersions 方法测试")
    class CheckVersionsTests {

        @BeforeEach
        void setUp() {
            when(providerGateway.getMaxVersion()).thenReturn(10L);
            when(modelGateway.getMaxVersion()).thenReturn(20L);
            when(apiKeyGateway.getMaxVersion()).thenReturn(30L);
            versionChecker.initVersions();
        }

        @Test
        @DisplayName("版本无变化不刷新缓存")
        void checkVersions_noChange_noRefresh() {
            // given - 版本不变
            when(providerGateway.getMaxVersion()).thenReturn(10L);
            when(modelGateway.getMaxVersion()).thenReturn(20L);
            when(apiKeyGateway.getMaxVersion()).thenReturn(30L);

            // when
            versionChecker.checkVersions();

            // then
            verify(cacheService, never()).refreshProviders();
            verify(cacheService, never()).refreshModels();
            verify(cacheService, never()).refreshApiKeys();
        }

        @Test
        @DisplayName("Provider 版本变化刷新缓存")
        void checkVersions_providerChanged_refreshesCache() {
            // given
            when(providerGateway.getMaxVersion()).thenReturn(15L); // 版本变化

            // when
            versionChecker.checkVersions();

            // then
            verify(cacheService).refreshProviders();
        }

        @Test
        @DisplayName("Model 版本变化刷新缓存")
        void checkVersions_modelChanged_refreshesCache() {
            // given
            when(modelGateway.getMaxVersion()).thenReturn(25L); // 版本变化

            // when
            versionChecker.checkVersions();

            // then
            verify(cacheService).refreshModels();
        }

        @Test
        @DisplayName("API Key 版本变化刷新缓存")
        void checkVersions_apiKeyChanged_refreshesCache() {
            // given
            when(apiKeyGateway.getMaxVersion()).thenReturn(35L); // 版本变化

            // when
            versionChecker.checkVersions();

            // then
            verify(cacheService).refreshApiKeys();
        }

        @Test
        @DisplayName("所有版本变化刷新所有缓存")
        void checkVersions_allChanged_refreshesAll() {
            // given
            when(providerGateway.getMaxVersion()).thenReturn(15L);
            when(modelGateway.getMaxVersion()).thenReturn(25L);
            when(apiKeyGateway.getMaxVersion()).thenReturn(35L);

            // when
            versionChecker.checkVersions();

            // then
            verify(cacheService).refreshProviders();
            verify(cacheService).refreshModels();
            verify(cacheService).refreshApiKeys();
        }
    }
}
