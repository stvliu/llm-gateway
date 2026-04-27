package com.codingas.gateway.adapter.model.controller;

import com.codingas.gateway.adapter.model.dto.CreateModelRequest;
import com.codingas.gateway.adapter.model.dto.ModelResponse;
import com.codingas.gateway.adapter.model.dto.UpdateModelRequest;
import com.codingas.gateway.application.model.ModelManageUseCase;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.router.entity.Model;
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
public class ModelController {

    private final ModelManageUseCase modelManageUseCase;

    @GetMapping
    public ApiResponse<List<ModelResponse>> list() {
        List<Model> models = modelManageUseCase.findAll();
        List<ModelResponse> response = models.stream()
                .map(ModelResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelResponse> getById(@PathVariable Long id) {
        return modelManageUseCase.findById(id)
                .map(m -> ApiResponse.success(ModelResponse.from(m)))
                .orElse(ApiResponse.error("NOT_FOUND", "Model not found: " + id));
    }

    @PostMapping
    public ApiResponse<ModelResponse> create(@Valid @RequestBody CreateModelRequest request) {
        Model model = request.toEntity();
        Model created = modelManageUseCase.create(model);
        return ApiResponse.success(ModelResponse.from(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModelRequest request) {
        Model model = request.toEntity();
        Model updated = modelManageUseCase.update(id, model);
        return ApiResponse.success(ModelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelManageUseCase.delete(id);
        return ApiResponse.success();
    }
}
