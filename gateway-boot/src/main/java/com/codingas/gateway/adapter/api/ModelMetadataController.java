package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.ModelMetadataService;
import com.codingas.gateway.application.metadata.dto.ModelMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型元数据 API
 */
@RestController
@RequestMapping("/api/v1/model-metadata")
@RequiredArgsConstructor
public class ModelMetadataController {

    private final ModelMetadataService modelMetadataService;

    /**
     * 分页查询模型元数据
     */
    @GetMapping
    public ResponseEntity<Page<ModelMetadataResponse>> list(
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ModelMetadataResponse> page = modelMetadataService.listModelMetadata(
            providerId,
            keyword,
            source != null ? MetadataSource.valueOf(source) : null,
            pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * 获取模型元数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ModelMetadataResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(modelMetadataService.getModelMetadata(id));
    }

    /**
     * 查询某供应商的所有模型
     */
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ModelMetadataResponse>> listByProviderId(
            @PathVariable String providerId) {
        return ResponseEntity.ok(modelMetadataService.listByProviderId(providerId));
    }

    /**
     * 创建模型元数据
     */
    @PostMapping
    public ResponseEntity<ModelMetadataResponse> create(
            @RequestBody ModelMetadata metadata) {
        return ResponseEntity.ok(modelMetadataService.createModelMetadata(metadata));
    }

    /**
     * 更新模型元数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<ModelMetadataResponse> update(
            @PathVariable Long id,
            @RequestBody ModelMetadata update) {
        return ResponseEntity.ok(modelMetadataService.updateModelMetadata(id, update));
    }

    /**
     * 删除模型元数据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modelMetadataService.deleteModelMetadata(id);
        return ResponseEntity.noContent().build();
    }
}