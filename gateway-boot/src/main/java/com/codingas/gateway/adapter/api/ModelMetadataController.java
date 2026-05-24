package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.ModelMetadataService;
import com.codingas.gateway.application.metadata.dto.ModelMetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.ModelMetadataResponse;
import com.codingas.gateway.application.metadata.dto.ModelMetadataUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<ModelMetadataResponse>> list(
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String keyword) {
        List<ModelMetadataResponse> list = modelMetadataService.listModelMetadata(
            keyword, providerId
        );
        return ResponseEntity.ok(list);
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
            @Valid @RequestBody ModelMetadataCreateRequest request) {
        return ResponseEntity.ok(modelMetadataService.createModelMetadata(request));
    }

    /**
     * 更新模型元数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<ModelMetadataResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ModelMetadataUpdateRequest request) {
        return ResponseEntity.ok(modelMetadataService.updateModelMetadata(id, request));
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