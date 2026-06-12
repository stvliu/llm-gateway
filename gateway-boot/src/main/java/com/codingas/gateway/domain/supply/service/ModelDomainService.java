package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 模型领域服务
 *
 * <p>封装模型相关的核心业务逻辑。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDomainService {

    private final ModelGateway modelGateway;

    /**
     * 创建模型
     */
    public Model create(Model model) {
        return modelGateway.save(model);
    }

    /**
     * 更新模型
     */
    public Model update(Model model) {
        return modelGateway.save(model);
    }

    /**
     * 启用模型
     */
    public Model enable(Long id) {
        Model model = modelGateway.findById(id)
                .orElseThrow(() -> new ProviderException("MODEL_NOT_FOUND", "模型不存在: " + id));
        model.setDeprecatedAt(null);
        return modelGateway.save(model);
    }

    /**
     * 废弃模型
     */
    public Model disable(Long id) {
        Model model = modelGateway.findById(id)
                .orElseThrow(() -> new ProviderException("MODEL_NOT_FOUND", "模型不存在: " + id));
        model.setDeprecatedAt(Instant.now());
        return modelGateway.save(model);
    }

    /**
     * 根据模型名查找
     */
    public Optional<Model> findByModelName(String modelName) {
        return modelGateway.findByModelName(modelName);
    }

    /**
     * 查找所有活跃模型
     */
    public List<Model> findAllActive() {
        return modelGateway.findAllActive();
    }

    /**
     * 查找所有模型
     */
    public List<Model> findAll() {
        return modelGateway.findAll();
    }
}