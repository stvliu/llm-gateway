package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public Model findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Model findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode).orElse(null);
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
    java.util.Optional<Model> findById(Long id);
    java.util.Optional<Model> findByModelCode(String modelCode);
    List<Model> findAllActive();
    List<Model> findByProviderId(Long providerId);
    Model save(Model model);
}