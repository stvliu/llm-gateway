package com.codingas.gateway.application.model;

import com.codingas.gateway.adapter.admin.dto.model.ModelCreateRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelResponse;
import com.codingas.gateway.adapter.admin.dto.model.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Model.ModelStatus;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型应用服务
 *
 * <p>处理模型管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelApplication {

    private final ModelGateway modelGateway;
    private final ProviderGateway providerGateway;

    /**
     * 创建模型
     */
    @Transactional
    public ModelResponse create(ModelCreateRequest request) {
        // 检查模型代码唯一性
        if (modelGateway.existsByModelCode(request.getModelCode())) {
            throw new DuplicateResourceException("Model", "modelCode");
        }

        // 查找提供商
        Provider provider = providerGateway.findById(request.getProviderId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));

        // 创建模型
        Model model = new Model();
        model.setModelCode(request.getModelCode());
        model.setProvider(provider);
        model.setProviderModelId(request.getProviderModelId());
        model.setDisplayName(request.getDisplayName());
        model.setContextWindow(request.getContextWindow());
        model.setInputPrice(request.getInputPrice());
        model.setOutputPrice(request.getOutputPrice());
        model.setCapabilities(request.getCapabilities());
        model.setStatus(ModelStatus.ACTIVE);

        Model savedModel = modelGateway.save(model);
        return toResponse(savedModel);
    }

    /**
     * 根据 ID 获取模型
     */
    public ModelResponse getById(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        return toResponse(model);
    }

    /**
     * 查询模型列表
     */
    public PageResponse<ModelResponse> query(ModelQueryRequest request) {
        List<Model> models = modelGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            models = models.stream()
                .filter(m -> m.getModelCode().toLowerCase().contains(keyword)
                    || (m.getDisplayName() != null && m.getDisplayName().toLowerCase().contains(keyword))
                    || m.getProviderModelId().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getProviderId() != null) {
            models = models.stream()
                .filter(m -> m.getProvider().getId().equals(request.getProviderId()))
                .collect(Collectors.toList());
        }

        if (request.getStatus() != null) {
            models = models.stream()
                .filter(m -> m.getStatus() == request.getStatus())
                .collect(Collectors.toList());
        }

        // 统计
        long total = models.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<Model> pagedModels = models.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<ModelResponse> responses = pagedModels.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新模型
     */
    @Transactional
    public ModelResponse update(Long id, ModelUpdateRequest request) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        if (request.getDisplayName() != null) {
            model.setDisplayName(request.getDisplayName());
        }
        if (request.getContextWindow() != null) {
            model.setContextWindow(request.getContextWindow());
        }
        if (request.getInputPrice() != null) {
            model.setInputPrice(request.getInputPrice());
        }
        if (request.getOutputPrice() != null) {
            model.setOutputPrice(request.getOutputPrice());
        }
        if (request.getCapabilities() != null) {
            model.setCapabilities(request.getCapabilities());
        }
        if (request.getEnabled() != null) {
            model.setStatus(request.getEnabled() ? ModelStatus.ACTIVE : ModelStatus.DEPRECATED);
        }

        return toResponse(modelGateway.save(model));
    }

    /**
     * 删除模型（软删除）
     */
    @Transactional
    public void delete(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        model.setDeletedAt(Instant.now());
        modelGateway.save(model);
    }

    /**
     * 启用/禁用模型
     */
    @Transactional
    public ModelResponse setEnabled(Long id, boolean enabled) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        model.setStatus(enabled ? ModelStatus.ACTIVE : ModelStatus.DEPRECATED);
        return toResponse(modelGateway.save(model));
    }

    /**
     * 转换为响应 DTO
     */
    private ModelResponse toResponse(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setModelCode(model.getModelCode());
        response.setProviderId(model.getProvider().getId());
        response.setProviderName(model.getProvider().getProviderName());
        response.setProviderCode(model.getProvider().getProviderCode());
        response.setProviderModelId(model.getProviderModelId());
        response.setDisplayName(model.getDisplayName());
        response.setContextWindow(model.getContextWindow());
        response.setInputPrice(model.getInputPrice());
        response.setOutputPrice(model.getOutputPrice());
        response.setCapabilities(model.getCapabilities());
        response.setStatus(model.getStatus());
        response.setEnabled(model.getStatus() == ModelStatus.ACTIVE);
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
        return response;
    }
}
