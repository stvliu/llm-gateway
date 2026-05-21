package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.codingas.gateway.application.metadata.MetadataSyncService;
import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 元数据同步 API
 * <p>
 * 管理级操作，仅限管理员访问。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/metadata-sync")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class MetadataSyncController {

    private final MetadataSyncService syncService;

    /**
     * 手动触发全量同步
     */
    @PostMapping("/all")
    public ResponseEntity<MetadataSyncResult> syncAll() {
        return ResponseEntity.ok(syncService.syncAll());
    }

    /**
     * 手动触发内置元数据同步
     */
    @PostMapping("/builtin")
    public ResponseEntity<MetadataSyncResult> syncBuiltin() {
        return ResponseEntity.ok(syncService.syncBuiltinMetadata());
    }

    /**
     * 手动触发 Models.dev 同步
     */
    @PostMapping("/models-dev")
    public ResponseEntity<MetadataSyncResult> syncModelsDev() {
        return ResponseEntity.ok(syncService.syncModelsDev());
    }
}