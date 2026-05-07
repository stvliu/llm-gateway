package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.application.template.ProviderTemplateService;
import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provider 模板管理接口
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class ProviderTemplateController {

    private final ProviderTemplateService service;

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
}