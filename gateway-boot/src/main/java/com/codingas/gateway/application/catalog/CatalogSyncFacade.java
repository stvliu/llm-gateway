/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog;

import com.codingas.gateway.infrastructure.supply.catalog.loader.BuiltinDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 目录同步服务
 *
 * <p>负责触发目录数据的同步操作，包括 BUILTIN 数据重新加载。</p>
 * <p>替代原 CatalogSyncService。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSyncFacade {

    private final BuiltinDataLoader builtinDataLoader;

    /**
     * 同步 BUILTIN 数据
     *
     * <p>强制重新加载 BUILTIN 目录数据。upsert 规则保证已有记录不会被重复创建。</p>
     * <p>可由 Controller 手动触发，用于运营人员刷新目录数据。</p>
     */
    public void syncBuiltin() {
        log.info("开始手动触发 BUILTIN 同步...");
        builtinDataLoader.forceReload();
        log.info("BUILTIN 同步完成");
    }
}