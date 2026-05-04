package com.codingas.gateway.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 配置加载器
 *
 * <p>应用启动时加载配置到缓存。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigLoader implements ApplicationRunner {

    private final ConfigCacheService cacheService;
    private final ConfigVersionChecker versionChecker;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading configuration into cache...");

        // 加载所有配置
        cacheService.refreshAll();

        // 初始化版本号
        versionChecker.initVersions();

        log.info("Configuration loaded successfully");
    }
}
