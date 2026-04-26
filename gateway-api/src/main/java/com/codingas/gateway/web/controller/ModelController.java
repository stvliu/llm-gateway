package com.codingas.gateway.web.controller;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.web.service.ModelManageUseCase;
import com.codingas.gateway.web.dto.ApiResponse;
import com.codingas.gateway.web.dto.CreateModelRequest;
import com.codingas.gateway.web.dto.ModelResponse;
import com.codingas.gateway.web.dto.UpdateModelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理 API
 *
 * <p>处理 Model 的 CRUD 操作。</p>
 */
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "模型管理接口")
public class ModelController {

    private final ModelManageUseCase modelManageUseCase;

    @GetMapping
    @Operation(summary = "获取模型列表")
    public ApiResponse<List<ModelResponse>> list(
            @RequestParam(required = false) Long providerId) {
        List<Model> models;
        if (providerId != null) {
            models = modelManageUseCase.findByProviderId(providerId);
        } else {
            models = modelManageUseCase.findAll();
        }
        List<ModelResponse> response = models.stream()
                .map(ModelResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模型详情")
    public ApiResponse<ModelResponse> getById(@PathVariable Long id) {
        return modelManageUseCase.findById(id)
                .map(m -> ApiResponse.success(ModelResponse.from(m)))
                .orElse(ApiResponse.error("NOT_FOUND", "Model not found: " + id));
    }

    @GetMapping("/code/{modelCode}")
    @Operation(summary = "根据编码获取模型")
    public ApiResponse<ModelResponse> getByCode(@PathVariable String modelCode) {
        return modelManageUseCase.findByModelCode(modelCode)
                .map(m -> ApiResponse.success(ModelResponse.from(m)))
                .orElse(ApiResponse.error("NOT_FOUND", "Model not found: " + modelCode));
    }

    @PostMapping
    @Operation(summary = "创建模型")
    public ApiResponse<ModelResponse> create(@Valid @RequestBody CreateModelRequest request) {
        Model model = request.toEntity();
        Model created = modelManageUseCase.create(model);
        return ApiResponse.success(ModelResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型")
    public ApiResponse<ModelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModelRequest request) {
        Model model = request.toEntity();
        Model updated = modelManageUseCase.update(id, model);
        return ApiResponse.success(ModelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelManageUseCase.delete(id);
        return ApiResponse.success();
    }
}
