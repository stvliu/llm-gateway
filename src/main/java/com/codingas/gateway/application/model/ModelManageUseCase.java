package com.codingas.gateway.application.model;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 模型管理用例编排器
 *
 * <p>Application 层用例编排，负责模型管理的请求处理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManageUseCase {

    private final ModelService modelService;

    /**
     * 查询所有模型
     */
    public List<Model> findAll() {
        log.debug("UseCase: find all models");
        return modelService.findAll();
    }

    /**
     * 根据 ID 查询模型
     */
    public Optional<Model> findById(Long id) {
        log.debug("UseCase: find model by id={}", id);
        return modelService.findById(id);
    }

    /**
     * 创建模型
     */
    public Model create(Model model) {
        log.info("UseCase: create model, code={}", model.getModelCode());
        return modelService.create(model);
    }

    /**
     * 更新模型
     */
    public Model update(Long id, Model model) {
        log.info("UseCase: update model, id={}", id);
        return modelService.update(id, model);
    }

    /**
     * 删除模型
     */
    public void delete(Long id) {
        log.info("UseCase: delete model, id={}", id);
        modelService.delete(id);
    }
}
