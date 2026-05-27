package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.modelspec.ModelSpecService;
import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型规格管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/model-specs")
@RequiredArgsConstructor
public class ModelSpecController {

    private final ModelSpecService modelSpecService;

    /**
     * 创建模型规格
     */
    @PostMapping
    public ResponseEntity<ModelSpecResponse> create(@Valid @RequestBody ModelSpecCreateRequest request) {
        ModelSpecResponse response = modelSpecService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 根据 ID 查询模型规格
     */
    @GetMapping("/{id}")
    public ResponseEntity<ModelSpecResponse> getById(@PathVariable Long id) {
        ModelSpecResponse response = modelSpecService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询模型规格列表
     */
    @GetMapping
    public ResponseEntity<List<ModelSpecResponse>> query(@Valid ModelSpecQueryRequest request) {
        List<ModelSpecResponse> responses = modelSpecService.query(request);
        return ResponseEntity.ok(responses);
    }

    /**
     * 更新模型规格
     */
    @PutMapping("/{id}")
    public ResponseEntity<ModelSpecResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ModelSpecUpdateRequest request) {
        ModelSpecResponse response = modelSpecService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除模型规格
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modelSpecService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用/禁用模型规格
     */
    @PatchMapping("/{id}/state")
    public ResponseEntity<ModelSpecResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        ModelSpecResponse response = modelSpecService.setEnabled(id, enabled);
        return ResponseEntity.ok(response);
    }
}
