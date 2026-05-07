package com.codingas.gateway.infrastructure.template.config;

import com.codingas.gateway.application.template.OfficialTemplateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时同步模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "template.git", name = "sync-on-startup", havingValue = "true", matchIfMissing = true)
public class TemplateSyncRunner implements ApplicationRunner {

    private final OfficialTemplateSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting official template sync on startup...");
            syncService.syncTemplates();
        } catch (Exception e) {
            log.warn("Failed to sync templates on startup, will use built-in templates", e);
        }
    }
}