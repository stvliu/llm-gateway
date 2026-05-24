package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelSpecGateway modelSpecGateway;
    private final ProviderGateway providerGateway;

    /**
     * 创建模型
     */
    @Override
    @Transactional
    public ModelResponse create(ModelCreateRequest request) {
        // 验证提供商存在
        if (providerGateway.findById(request.getProviderId()).isEmpty()) {
            throw new ResourceNotFoundException("Provider", request.getProviderId());
        }

        // 创建模型规格
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setProviderId(request.getProviderId());
        modelSpec.setProviderModelId(request.getProviderModelId());
        modelSpec.setDisplayName(request.getDisplayName());
        modelSpec.setContextWindow(request.getContextWindow());
        modelSpec.setCapabilities(request.getCapabilities());
        modelSpec.setState(ModelSpecState.ACTIVE);
        // 路由字段
        modelSpec.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        modelSpec.setWeight(request.getWeight() != null ? request.getWeight() : 100);

        ModelSpec savedModel = modelSpecGateway.save(modelSpec);
        return toResponse(savedModel);
    }

    /**
     * 根据 ID 获取模型
     */
    @Override
    public ModelResponse getById(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        return toResponse(modelSpec);
    }

    /**
     * 查询模型列表
     */
    @Override
    public PageResponse<ModelResponse> query(ModelQueryRequest request) {
        List<ModelSpec> models = modelSpecGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            models = models.stream()
                .filter(m -> (m.getDisplayName() != null && m.getDisplayName().toLowerCase().contains(keyword))
                    || m.getProviderModelId().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getProviderId() != null) {
            models = models.stream()
                .filter(m -> request.getProviderId().equals(m.getProviderId()))
                .collect(Collectors.toList());
        }

        if (request.getState() != null) {
            models = models.stream()
                .filter(m -> m.getState().equals(request.getState()))
                .collect(Collectors.toList());
        }

        // 统计
        long total = models.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<ModelSpec> pagedModels = models.stream()
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
    @Override
    @Transactional
    public ModelResponse update(Long id, ModelUpdateRequest request) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));

        if (request.getDisplayName() != null) {
            modelSpec.setDisplayName(request.getDisplayName());
        }
        if (request.getContextWindow() != null) {
            modelSpec.setContextWindow(request.getContextWindow());
        }
        if (request.getCapabilities() != null) {
            modelSpec.setCapabilities(request.getCapabilities());
        }
        if (request.getState() != null) {
            modelSpec.setState(request.getState());
        }
        if (request.getPriority() != null) {
            modelSpec.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            modelSpec.setWeight(request.getWeight());
        }

        return toResponse(modelSpecGateway.save(modelSpec));
    }

    /**
     * 删除模型
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        modelSpecGateway.delete(modelSpec);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    @Transactional
    public ModelResponse setEnabled(Long id, boolean enabled) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        modelSpec.setState(enabled ? ModelSpecState.ACTIVE : ModelSpecState.DISABLED);
        return toResponse(modelSpecGateway.save(modelSpec));
    }

    /**
     * 转换为响应 DTO
     */
    private ModelResponse toResponse(ModelSpec modelSpec) {
        ModelResponse response = new ModelResponse();
        response.setId(modelSpec.getId());
        response.setProviderId(modelSpec.getProviderId());
        response.setProviderModelId(modelSpec.getProviderModelId());
        response.setDisplayName(modelSpec.getDisplayName());
        response.setContextWindow(modelSpec.getContextWindow());
        response.setCapabilities(modelSpec.getCapabilities());
        response.setState(modelSpec.getState());
        // 路由字段
        response.setPriority(modelSpec.getPriority());
        response.setWeight(modelSpec.getWeight());
        response.setCreatedAt(modelSpec.getCreatedAt());
        response.setUpdatedAt(modelSpec.getUpdatedAt());
        // 补充供应商名称
        if (modelSpec.getProviderId() != null) {
            providerGateway.findById(modelSpec.getProviderId())
                .ifPresent(p -> response.setProviderName(p.getName()));
        }
        return response;
    }
}