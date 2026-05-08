package com.codingas.gateway.infrastructure.template.config;

import com.codingas.gateway.application.template.OfficialTemplateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时同步内置模板到数据库
 *
 * <p>从 classpath:templates/ 目录加载预置模板并同步到数据库。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "template.builtin", name = "sync-on-startup", havingValue = "true", matchIfMissing = true)
public class BuiltinTemplateSyncRunner implements ApplicationRunner {

    private final OfficialTemplateSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== BuiltinTemplateSyncRunner started ==========");
        try {
            log.info("Starting builtin template sync on startup...");
            var result = syncService.syncBuiltinTemplates();
            log.info("Builtin template sync completed: {} total, {} added, {} updated",
                result.syncedCount(), result.addedCount(), result.updatedCount());
        } catch (Exception e) {
            log.error("Failed to sync builtin templates on startup", e);
        }
        log.info("========== BuiltinTemplateSyncRunner finished ==========");
    }
}
