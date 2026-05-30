package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelGatewayImpl implements ModelGateway {

    private final ModelRepository modelRepository;

    @Override
    public Model save(Model model) {
        ModelDo doObj = toDo(model);
        ModelDo saved = modelRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<Model> findById(Long id) {
        return modelRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Model> findByModelName(String modelName) {
        return modelRepository.findByModelName(modelName).map(this::toEntity);
    }

    @Override
    public List<Model> findActiveByModelName(String modelName) {
        return modelRepository.findByModelNameAndState(modelName, ModelState.ACTIVE.name())
                .stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<Model> findAllActive() {
        return modelRepository.findByState(ModelState.ACTIVE.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<Model> findByIds(List<Long> ids) {
        return modelRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public long count() {
        return modelRepository.count();
    }

    @Override
    public void delete(Model model) {
        modelRepository.deleteById(model.getId());
    }

    private Model toEntity(ModelDo doObj) {
        Model entity = new Model();
        entity.setId(doObj.getId());
        entity.setModelName(doObj.getModelName());
        entity.setDisplayName(doObj.getDisplayName());
        entity.setModelFamily(doObj.getModelFamily());
        entity.setContextWindow(doObj.getContextWindow());
        entity.setMaxInputTokens(doObj.getMaxInputTokens());
        entity.setMaxOutputTokens(doObj.getMaxOutputTokens());
        entity.setCapabilities(doObj.getCapabilities());
        entity.setModalities(doObj.getModalities());
        entity.setState(ModelState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelDo toDo(Model entity) {
        ModelDo doObj = new ModelDo();
        doObj.setId(entity.getId());
        doObj.setModelName(entity.getModelName());
        doObj.setDisplayName(entity.getDisplayName());
        doObj.setModelFamily(entity.getModelFamily());
        doObj.setContextWindow(entity.getContextWindow());
        doObj.setMaxInputTokens(entity.getMaxInputTokens());
        doObj.setMaxOutputTokens(entity.getMaxOutputTokens());
        doObj.setCapabilities(entity.getCapabilities());
        doObj.setModalities(entity.getModalities());
        doObj.setState(entity.getState() != null ? entity.getState().name() : ModelState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}