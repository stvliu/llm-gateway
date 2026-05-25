package com.codingas.gateway.application.catalog;

import com.codingas.gateway.infrastructure.supply.catalog.loader.BuiltinCatalogLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 目录同步服务
 *
 * <p>负责触发目录数据的同步操作，包括 BUILTIN 数据重新加载和 Models.dev API 同步。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSyncService {

    private final BuiltinCatalogLoader builtinCatalogLoader;

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
     */
    public void syncModelsDev() {
        // TODO: 实现 Models.dev API 同步
        log.info("Models.dev 同步尚未实现");
    }
}
