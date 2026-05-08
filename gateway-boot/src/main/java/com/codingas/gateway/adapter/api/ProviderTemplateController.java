package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.template.OfficialTemplateSyncService;
import com.codingas.gateway.application.template.ProviderTemplateService;
import com.codingas.gateway.application.template.dto.ApplyTemplateRequest;
import com.codingas.gateway.application.template.dto.ApplyTemplateResult;
import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Provider 模板管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class ProviderTemplateController {

    private final ProviderTemplateService service;
    private final OfficialTemplateSyncService syncService;

    /**
     * 分页查询模板列表
     */
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> listTemplates(
            @RequestParam(required = false) TemplateType type,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketStatus marketStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        limit = Math.min(limit, 100);
        Page<TemplateResponse> result = service.listTemplates(type, providerType, keyword, marketStatus, page, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable Long id) {
        TemplateResponse response = service.getTemplate(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建自定义模板
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody TemplateCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-Username", defaultValue = "admin") String username) {

        TemplateResponse response = service.createTemplate(request, userId, username);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUpdateRequest request) {

        TemplateResponse response = service.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布到公共市场
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishTemplate(@PathVariable Long id) {
        service.publishTemplate(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 应用模板创建 Provider
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplyTemplateResult> applyTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ApplyTemplateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {

        ApplyTemplateResult result = service.applyTemplate(id, request, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 导出单个模板为 JSON 文件
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<Void> exportTemplate(@PathVariable Long id, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=template_" + id + ".json");
        service.exportTemplate(id, response.getOutputStream());
        return ResponseEntity.ok().build();
    }

    /**
     * 批量导出模板为 ZIP 文件
     */
    @GetMapping("/export/batch")
    public ResponseEntity<Void> exportTemplates(
            @RequestParam List<Long> ids,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=templates.zip");
        service.exportTemplates(ids, response.getOutputStream());
        return ResponseEntity.ok().build();
    }

    /**
     * 从 ZIP 文件导入模板
     */
    @PostMapping("/import")
    public ResponseEntity<List<TemplateResponse>> importTemplates(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-Username", defaultValue = "admin") String username) throws IOException {

        List<TemplateResponse> results = service.importTemplates(file.getInputStream(), userId, username);
        return ResponseEntity.ok(results);
    }

    /**
     * 手动同步内置模板
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncBuiltinTemplates() {
        try {
            OfficialTemplateSyncService.SyncResult result = syncService.syncBuiltinTemplates();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Sync failed", e);
            return ResponseEntity.internalServerError()
                .body(java.util.Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
                ));
        }
    }

}