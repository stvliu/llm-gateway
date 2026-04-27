package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关 JPA 实现
 *
 * <p>实现 ModelGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaModelGateway implements ModelGateway {

    private final ModelRepository repository;

    @Override
    public Optional<Model> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Model> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode);
    }

    @Override
    public List<Model> findAllActive() {
        return repository.findAllActive();
    }

    @Override
    public List<Model> findByProviderId(Long providerId) {
        return repository.findByProviderId(providerId);
    }

    @Override
    public Model save(Model model) {
        return repository.save(model);
    }
}

/**
 * 模型仓储接口
 */
interface ModelRepository {
    Optional<Model> findById(Long id);
    Optional<Model> findByModelCode(String modelCode);
    List<Model> findAllActive();
    List<Model> findByProviderId(Long providerId);
    Model save(Model model);
}
