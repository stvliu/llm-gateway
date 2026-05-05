package com.codingas.gateway.domain.model.service;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 模型领域服务
 *
 * <p>封装 Model 和 Provider Gateway 的访问，提供模型查询的业务规则。</p>
 * <p>Domain 层服务，只依赖 model 域内的 Gateway，不跨域访问。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDomainService {

    private final ModelGateway modelGateway;
    private final ProviderGateway providerGateway;

    // ==================== 查询方法 ====================

    /**
     * 查找所有活跃模型
     */
    @Transactional(readOnly = true)
    public List<Model> findAll() {
        return modelGateway.findAllActive();
    }

    /**
     * 根据 ID 查找模型
     */
    @Transactional(readOnly = true)
    public Optional<Model> findById(Long id) {
        return modelGateway.findById(id);
    }

    /**
     * 根据模型代码查找模型
     */
    @Transactional(readOnly = true)
    public Optional<Model> findByModelCode(String modelCode) {
        return modelGateway.findByModelCode(modelCode);
    }

    /**
     * 根据模型代码查找模型，不存在则抛出异常
     */
    @Transactional(readOnly = true)
    public Model getByModelCode(String modelCode) {
        return modelGateway.findByModelCode(modelCode)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelCode));
    }

    /**
     * 根据模型代码获取模型及其提供商信息
     *
     * <p>封装 Model 和 Provider 的关联查询，避免调用方分别访问 Gateway。</p>
     *
     * @param modelCode 模型代码
     * @return 模型与提供商信息
     * @throws NoSuchElementException 模型或提供商不存在
     */
    @Transactional(readOnly = true)
    public ModelProviderInfo getModelWithProvider(String modelCode) {
        Model model = modelGateway.findByModelCode(modelCode)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelCode));

        Long providerId = model.getProvider() != null ? model.getProvider().getId() : null;
        if (providerId == null) {
            throw new NoSuchElementException("Model has no provider: " + modelCode);
        }

        Provider provider = providerGateway.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found: " + providerId));

        return new ModelProviderInfo(model, provider);
    }

    /**
     * 根据提供商 ID 查找模型
     */
    @Transactional(readOnly = true)
    public List<Model> findByProviderId(Long providerId) {
        return modelGateway.findByProviderId(providerId);
    }

    // ==================== 命令方法 ====================

    /**
     * 创建模型
     */
    @Transactional
    public Model create(Model model) {
        Model saved = modelGateway.save(model);
        log.info("Created model: {} ({})", saved.getDisplayName(), saved.getModelCode());
        return saved;
    }

    /**
     * 更新模型
     */
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

    /**
     * 删除模型
     */
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

    /**
     * 保存模型
     */
    @Transactional
    public Model save(Model model) {
        return modelGateway.save(model);
    }

    // ==================== 值对象 ====================

    /**
     * 模型与提供商信息
     *
     * <p>值对象，封装一次查询返回的关联数据。</p>
     */
    public record ModelProviderInfo(Model model, Provider provider) {}
}
