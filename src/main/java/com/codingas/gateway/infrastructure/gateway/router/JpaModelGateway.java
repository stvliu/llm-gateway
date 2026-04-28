package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关 JPA 实现
 *
 * <p>实现 ModelGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaModelGateway implements ModelGateway {

    private final ModelRepository modelRepository;

    @Override
    public Model save(Model model) {
        return modelRepository.save(model);
    }

    @Override
    public Optional<Model> findById(Long id) {
        return modelRepository.findById(id);
    }

    @Override
    public Optional<Model> findByModelCode(String modelCode) {
        return modelRepository.findByModelCode(modelCode);
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAll();
    }

    @Override
    public List<Model> findAllActive() {
        return modelRepository.findAllActive();
    }

    @Override
    public List<Model> findByProviderId(Long providerId) {
        return modelRepository.findByProviderId(providerId);
    }

    @Override
    public long count() {
        return modelRepository.count();
    }

    @Override
    public void delete(Model model) {
        modelRepository.delete(model);
    }

    @Override
    public boolean existsByModelCode(String modelCode) {
        return modelRepository.existsByModelCode(modelCode);
    }
}
