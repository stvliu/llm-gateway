package com.codingas.gateway.infrastructure.metadata.config;

import com.codingas.gateway.application.metadata.MetadataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时同步内置元数据到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "metadata.builtin", name = "sync-on-startup", havingValue = "true", matchIfMissing = true)
public class BuiltinMetadataSyncRunner implements ApplicationRunner {

    private final MetadataSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== BuiltinMetadataSyncRunner started ==========");
        try {
            var result = syncService.syncBuiltinMetadata();
            log.info("Builtin metadata sync completed: {} total, {} added, {} updated",
                result.getSyncedCount(), result.getAddedCount(), result.getUpdatedCount());
        } catch (Exception e) {
            log.error("Failed to sync builtin metadata on startup", e);
        }
        log.info("========== BuiltinMetadataSyncRunner finished ==========");
    }
}