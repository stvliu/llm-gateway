package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.*;

/**
 * ConfigLoader 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigLoader 测试")
class ConfigLoaderTest {

    @Mock
    private ConfigCacheService cacheService;

    @Mock
    private ConfigVersionChecker versionChecker;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private ConfigLoader configLoader;

    @Test
    @DisplayName("应用启动时加载配置")
    void run_onStartup_loadsConfig() {
        // when
        configLoader.run(args);

        // then
        verify(cacheService).refreshAll();
        verify(versionChecker).initVersions();
    }
}
