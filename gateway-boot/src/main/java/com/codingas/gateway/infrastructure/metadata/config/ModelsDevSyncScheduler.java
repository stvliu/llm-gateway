package com.codingas.gateway.infrastructure.metadata.config;

import com.codingas.gateway.application.metadata.MetadataSyncService;
import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Models.dev 定时同步调度器
 * <p>
 * 根据配置的 syncInterval 定时触发 Models.dev 数据同步。
 * 默认每24小时同步一次，可通过 metadata.models-dev.sync-interval 调整。
 * 仅当 metadata.models-dev.enabled=true 时激活。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "metadata.models-dev", name = "enabled", havingValue = "true")
public class ModelsDevSyncScheduler {

    private final MetadataSyncService syncService;
    private final MetadataSyncConfig config;

    /**
     * 定时同步 Models.dev 数据
     * <p>
     * 使用配置的 syncInterval 作为固定延迟间隔。
     * 初次延迟 5 分钟，避免启动时与 BuiltinMetadataSyncRunner 冲突。
     * </p>
     */
    @Scheduled(
        initialDelayString = "${metadata.models-dev.initial-delay:300000}",
        fixedDelayString = "${metadata.models-dev.sync-interval:86400000}"
    )
    public void scheduledSync() {
        if (!config.getModelsDev().isEnabled()) {
            return;
        }

        log.info("========== Scheduled Models.dev sync started ==========");
        try {
            MetadataSyncResult result = syncService.syncModelsDev();
            log.info("Scheduled Models.dev sync completed: {} total, {} added, {} updated",
                result.getSyncedCount(), result.getAddedCount(), result.getUpdatedCount());
        } catch (Exception e) {
            log.error("Scheduled Models.dev sync failed", e);
        }
        log.info("========== Scheduled Models.dev sync finished ==========");
    }
}