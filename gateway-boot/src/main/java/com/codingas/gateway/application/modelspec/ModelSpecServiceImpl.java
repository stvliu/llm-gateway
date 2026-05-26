package com.codingas.gateway.application.modelspec;

import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型规格应用服务实现
 *
 * <p>管理模型规格（ModelSpec）的 CRUD 操作。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelSpecServiceImpl implements ModelSpecService {

    private final ModelSpecGateway modelSpecGateway;

    @Override
    @Transactional
    public ModelSpecResponse create(ModelSpecCreateRequest request) {
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setProviderId(request.getProviderId());
        modelSpec.setProviderModelId(request.getProviderModelId());
        modelSpec.setDisplayName(request.getDisplayName());
        modelSpec.setModelFamily(request.getModelFamily());
        modelSpec.setContextWindow(request.getContextWindow());
        modelSpec.setMaxInputTokens(request.getMaxInputTokens());
        modelSpec.setMaxOutputTokens(request.getMaxOutputTokens());
        modelSpec.setCapabilities(request.getCapabilities());
        modelSpec.setModalities(request.getModalities());
        modelSpec.setPriority(request.getPriority());
        modelSpec.setWeight(request.getWeight());
        modelSpec.setState(ModelSpecState.ACTIVE);

        ModelSpec saved = modelSpecGateway.save(modelSpec);
        log.info("Created model spec: id={}, providerModelId={}", saved.getId(), saved.getProviderModelId());

        return toResponse(saved);
    }

    @Override
    public ModelSpecResponse getById(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        return toResponse(modelSpec);
    }

    @Override
    public List<ModelSpecResponse> query(ModelSpecQueryRequest request) {
        List<ModelSpec> specs;

        if (request.getProviderId() != null) {
            specs = modelSpecGateway.findByProviderId(request.getProviderId());
        } else {
            specs = modelSpecGateway.findAll();
        }

        // 按状态过滤
        if (request.getState() != null) {
            ModelSpecState filterState = ModelSpecState.valueOf(request.getState());
            specs = specs.stream()
                    .filter(s -> s.getState() == filterState)
                    .toList();
        }

        // 按关键词过滤（匹配 providerModelId 或 displayName）
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            specs = specs.stream()
                    .filter(s -> {
                        String pmid = s.getProviderModelId();
                        String dn = s.getDisplayName();
                        return (pmid != null && pmid.toLowerCase().contains(keyword))
                                || (dn != null && dn.toLowerCase().contains(keyword));
                    })
                    .toList();
        }

        return specs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ModelSpecResponse update(Long id, ModelSpecUpdateRequest request) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));

        if (request.getProviderModelId() != null) {
            modelSpec.setProviderModelId(request.getProviderModelId());
        }
        if (request.getDisplayName() != null) {
            modelSpec.setDisplayName(request.getDisplayName());
        }
        if (request.getModelFamily() != null) {
            modelSpec.setModelFamily(request.getModelFamily());
        }
        if (request.getContextWindow() != null) {
            modelSpec.setContextWindow(request.getContextWindow());
        }
        if (request.getMaxInputTokens() != null) {
            modelSpec.setMaxInputTokens(request.getMaxInputTokens());
        }
        if (request.getMaxOutputTokens() != null) {
            modelSpec.setMaxOutputTokens(request.getMaxOutputTokens());
        }
        if (request.getCapabilities() != null) {
            modelSpec.setCapabilities(request.getCapabilities());
        }
        if (request.getModalities() != null) {
            modelSpec.setModalities(request.getModalities());
        }
        if (request.getPriority() != null) {
            modelSpec.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            modelSpec.setWeight(request.getWeight());
        }
        if (request.getState() != null) {
            modelSpec.setState(ModelSpecState.valueOf(request.getState()));
        }

        ModelSpec saved = modelSpecGateway.save(modelSpec);
        log.info("Updated model spec: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        modelSpecGateway.delete(modelSpec);
        log.info("Deleted model spec: id={}", id);
    }

    @Override
    @Transactional
    public ModelSpecResponse setEnabled(Long id, boolean enabled) {
        ModelSpec modelSpec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        modelSpec.setState(enabled ? ModelSpecState.ACTIVE : ModelSpecState.INACTIVE);

        ModelSpec saved = modelSpecGateway.save(modelSpec);
        log.info("Set model spec enabled: id={}, enabled={}", id, enabled);

        return toResponse(saved);
    }

    /**
     * 实体转响应 DTO
     */
    private ModelSpecResponse toResponse(ModelSpec modelSpec) {
        ModelSpecResponse response = new ModelSpecResponse();
        response.setId(modelSpec.getId());
        response.setProviderId(modelSpec.getProviderId());
        response.setProviderModelId(modelSpec.getProviderModelId());
        response.setDisplayName(modelSpec.getDisplayName());
        response.setModelFamily(modelSpec.getModelFamily());
        response.setContextWindow(modelSpec.getContextWindow());
        response.setMaxInputTokens(modelSpec.getMaxInputTokens());
        response.setMaxOutputTokens(modelSpec.getMaxOutputTokens());
        response.setCapabilities(modelSpec.getCapabilities());
        response.setModalities(modelSpec.getModalities());
        response.setState(modelSpec.getState().name());
        response.setPriority(modelSpec.getPriority());
        response.setWeight(modelSpec.getWeight());
        response.setCreatedAt(modelSpec.getCreatedAt());
        response.setUpdatedAt(modelSpec.getUpdatedAt());
        return response;
    }
}
