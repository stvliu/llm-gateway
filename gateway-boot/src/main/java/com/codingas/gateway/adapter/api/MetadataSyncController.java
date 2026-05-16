package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.MetadataSyncService;
import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 元数据同步 API
 * <p>
 * 管理级操作，需要登录认证。
 * TODO: 角色权限系统完善后，应限制为 ADMIN 角色（@SaCheckRole("ADMIN")）
 * </p>
 */
@RestController
@RequestMapping("/api/v1/metadata-sync")
@RequiredArgsConstructor
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