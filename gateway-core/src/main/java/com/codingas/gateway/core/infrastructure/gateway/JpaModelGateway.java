package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.domain.gateway.ModelGateway;
import com.codingas.gateway.core.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关实现
 *
 * <p>实现 ModelGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaModelGateway implements ModelGateway {

    private final ModelRepository repository;

    @Override
    public Optional<Model> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode);
    }

    @Override
    public List<Model> findByProviderCode(String providerCode) {
        // 需要通过 Provider 查找，这里简化处理
        return repository.findAll().stream()
                .filter(m -> m.getProviderId() != null)
                .toList();
    }

    @Override
    public List<Model> findByRouteGroupId(Long routeGroupId) {
        // RouteGroup 与 Model 的关联需要通过 RouteGroupProvider 表
        // 这里简化处理
        return repository.findAll().stream()
                .filter(m -> routeGroupId != null)
                .toList();
    }

    @Override
    public Optional<Model> findById(Long modelId) {
        return repository.findById(modelId);
    }

    @Override
    public Model save(Model model) {
        return repository.save(model);
    }

    @Override
    public List<Model> findAllActive() {
        return repository.findAll().stream()
                .filter(m -> m.getStatus() == Model.ModelStatus.ACTIVE)
                .toList();
    }
}
