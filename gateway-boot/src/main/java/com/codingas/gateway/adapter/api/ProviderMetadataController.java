package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.ProviderMetadataService;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProviderMetadataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商元数据 API
 */
@RestController
@RequestMapping("/api/v1/provider-metadata")
@RequiredArgsConstructor
public class ProviderMetadataController {

    private final ProviderMetadataService providerMetadataService;

    /**
     * 分页查询供应商元数据
     */
    @GetMapping
    public ResponseEntity<Page<ProviderMetadataResponse>> list(
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProviderMetadataResponse> page = providerMetadataService.listProviderMetadata(
            providerType,
            keyword,
            pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * 获取供应商元数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderMetadataResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(providerMetadataService.getProviderMetadata(id));
    }

    /**
     * 获取所有供应商元数据
     */
    @GetMapping("/list")
    public ResponseEntity<List<ProviderMetadataResponse>> listAll() {
        return ResponseEntity.ok(providerMetadataService.listAllMetadata());
    }

    /**
     * 创建供应商元数据
     */
    @PostMapping
    public ResponseEntity<ProviderMetadataResponse> create(
            @Valid @RequestBody MetadataCreateRequest request) {
        return ResponseEntity.ok(providerMetadataService.createMetadata(request));
    }

    /**
     * 更新供应商元数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProviderMetadataResponse> update(
            @PathVariable Long id,
            @RequestBody MetadataUpdateRequest request) {
        return ResponseEntity.ok(providerMetadataService.updateMetadata(id, request));
    }

    /**
     * 删除供应商元数据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        providerMetadataService.deleteMetadata(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 应用元数据：创建供应商实例
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> apply(
            @PathVariable Long id,
            @Valid @RequestBody ApplyMetadataRequest request) {
        return ResponseEntity.ok(providerMetadataService.applyMetadata(id, request));
    }
}
