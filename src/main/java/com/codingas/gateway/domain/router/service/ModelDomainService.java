package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
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
public class ModelDomainService {

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
        return modelGateway.findByProviderId(providerId);
    }

    @Transactional
    public Model create(Model model) {
        Model saved = modelGateway.save(model);
        log.info("Created model: {} ({})", saved.getDisplayName(), saved.getModelCode());
        return saved;
    }

    @Transactional
    public Model update(Long id, Model model) {
        Optional<Model> existingOpt = modelGateway.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Model not found: " + id);
        }

        Model existing = existingOpt.get();
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
        Optional<Model> modelOpt = modelGateway.findById(id);
        if (modelOpt.isEmpty()) {
            throw new IllegalArgumentException("Model not found: " + id);
        }
        Model model = modelOpt.get();
        model.setStatus(Model.ModelStatus.DELETED);
        modelGateway.save(model);
        log.info("Deleted model: {}", id);
    }
}
