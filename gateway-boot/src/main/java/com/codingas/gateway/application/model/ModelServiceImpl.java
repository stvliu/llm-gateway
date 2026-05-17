package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
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

    private final ModelGateway modelGateway;
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

        // 创建模型
        Model model = new Model();
        model.setProviderId(request.getProviderId());
        model.setProviderModelId(request.getProviderModelId());
        model.setDisplayName(request.getDisplayName());
        model.setContextWindow(request.getContextWindow());
        model.setInputPrice(request.getInputPrice());
        model.setOutputPrice(request.getOutputPrice());
        model.setCapabilities(request.getCapabilities());
        model.setState(ModelState.ACTIVE);
        // 路由字段
        model.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        model.setWeight(request.getWeight() != null ? request.getWeight() : 100);

        Model savedModel = modelGateway.save(model);
        return toResponse(savedModel);
    }

    /**
     * 根据 ID 获取模型
     */
    @Override
    public ModelResponse getById(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        return toResponse(model);
    }

    /**
     * 查询模型列表
     */
    @Override
    public PageResponse<ModelResponse> query(ModelQueryRequest request) {
        List<Model> models = modelGateway.findAll();

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
                .filter(m -> m.getProviderId().equals(request.getProviderId()))
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
    @Override
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
        if (request.getState() != null) {
            model.setState(request.getState());
        }
        if (request.getPriority() != null) {
            model.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            model.setWeight(request.getWeight());
        }

        return toResponse(modelGateway.save(model));
    }

    /**
     * 删除模型
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        modelGateway.delete(model);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    @Transactional
    public ModelResponse setEnabled(Long id, boolean enabled) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        model.setState(enabled ? ModelState.ACTIVE : ModelState.DISABLED);
        return toResponse(modelGateway.save(model));
    }

    /**
     * 转换为响应 DTO
     */
    private ModelResponse toResponse(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setProviderId(model.getProviderId());
        response.setProviderName(model.getProviderName());
        response.setProviderModelId(model.getProviderModelId());
        response.setDisplayName(model.getDisplayName());
        response.setContextWindow(model.getContextWindow());
        response.setInputPrice(model.getInputPrice());
        response.setOutputPrice(model.getOutputPrice());
        response.setCapabilities(model.getCapabilities());
        response.setState(model.getState());
        // 路由字段
        response.setPriority(model.getPriority());
        response.setWeight(model.getWeight());
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
        return response;
    }
}