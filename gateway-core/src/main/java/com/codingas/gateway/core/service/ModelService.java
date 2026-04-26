package com.codingas.gateway.core.service;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.domain.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 模型服务
 *
 * <p>处理 Model 的 CRUD 操作和 Provider-Model 关联管理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelGateway modelGateway;

    @Transactional(readOnly = true)
    public List<Model> findAll() {
        return modelGateway.findAllActive();
    }

    @Transactional(readOnly = true)
    public Optional<Model> findById(Long id) {
        return modelGateway.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Model> findByModelCode(String modelCode) {
        return modelGateway.findByModelCode(modelCode);
    }

    @Transactional(readOnly = true)
    public List<Model> findByProviderId(Long providerId) {
        return modelGateway.findByProviderCode(String.valueOf(providerId));
    }

    @Transactional
    public Model create(Model model) {
        Model saved = modelGateway.save(model);
        log.info("Created model: {} ({})", saved.getDisplayName(), saved.getModelCode());
        return saved;
    }

    @Transactional
    public Model update(Long id, Model model) {
        Model existing = modelGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

        existing.setDisplayName(model.getDisplayName());
        existing.setContextWindow(model.getContextWindow());
        existing.setInputPrice(model.getInputPrice());
        existing.setOutputPrice(model.getOutputPrice());
        existing.setCapabilities(model.getCapabilities());
        existing.setStatus(model.getStatus());

        Model updated = modelGateway.save(existing);
        log.info("Updated model: {} ({})", updated.getDisplayName(), updated.getModelCode());
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Model model = modelGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
        modelGateway.save(model);
        log.info("Deleted model: {}", id);
    }
}