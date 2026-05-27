package com.codingas.gateway.application.catalog;

import com.codingas.gateway.infrastructure.supply.catalog.loader.BuiltinCatalogLoader;
import com.codingas.gateway.infrastructure.supply.catalog.sync.ModelsDevSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 目录同步服务
 *
 * <p>负责触发目录数据的同步操作，包括 BUILTIN 数据重新加载和 Models.dev 同步。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSyncService {

    private final BuiltinCatalogLoader builtinCatalogLoader;
    private final ModelsDevSyncClient modelsDevSyncClient;

    /**
     * 同步 BUILTIN 数据
     *
     * <p>强制重新加载 BUILTIN 目录数据。upsert 规则保证已有记录不会被重复创建。</p>
     * <p>可由 Controller 手动触发，用于运营人员刷新目录数据。</p>
     */
    public void syncBuiltin() {
        log.info("开始手动触发 BUILTIN 同步...");
        builtinCatalogLoader.forceReload();
        log.info("BUILTIN 同步完成");
    }

    /**
     * 同步 Models.dev 数据
     *
     * <p>从 Models.dev 数据源加载目录数据，逐条 upsert 并标记已消失的条目为 DEPRECATED。</p>
     */
    public void syncModelsDev() {
        log.info("开始 Models.dev 同步...");
        var result = modelsDevSyncClient.sync();
        log.info("Models.dev 同步完成: added={}, updated={}, skipped={}",
                result.totalAdded(), result.totalUpdated(),
                result.skippedProviders() + result.skippedModelSpecs() + result.skippedPlans() + result.skippedPlanModels());
    }
}
