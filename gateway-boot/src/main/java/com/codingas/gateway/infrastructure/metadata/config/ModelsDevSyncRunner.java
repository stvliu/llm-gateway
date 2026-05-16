package com.codingas.gateway.infrastructure.metadata.config;

import com.codingas.gateway.application.metadata.MetadataSyncService;
import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时同步 Models.dev 模型元数据
 * <p>
 * 在 BuiltinMetadataSyncRunner 之后执行（Order=2），
 * 确保内置供应商元数据已就绪再同步 Models.dev 数据。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
@ConditionalOnProperty(prefix = "metadata.models-dev", name = "sync-on-startup", havingValue = "true")
public class ModelsDevSyncRunner implements ApplicationRunner {

    private final MetadataSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== ModelsDevSyncRunner started ==========");
        try {
            MetadataSyncResult result = syncService.syncModelsDev();
            log.info("Models.dev sync completed: {} total, {} added, {} updated",
                result.getSyncedCount(), result.getAddedCount(), result.getUpdatedCount());
        } catch (Exception e) {
            log.error("Failed to sync Models.dev metadata on startup", e);
        }
        log.info("========== ModelsDevSyncRunner finished ==========");
    }
}
