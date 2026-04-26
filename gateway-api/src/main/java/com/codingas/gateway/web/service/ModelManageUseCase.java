package com.codingas.gateway.web.service;

import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 模型管理用例编排器
 *
 * <p>Application 层用例编排器，负责 Model 的 CRUD 操作。
 * <p>遵循 COLA 5.0 架构：接收 Controller 的 DTO，编排 Domain Service，返回响应 DTO。
 *
 * <p>职责：
 * <ul>
 *   <li>编排 ModelService 实现 CRUD 操作</li>
 *   <li>处理业务异常转换</li>
 *   <li>记录审计日志</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManageUseCase {

    private final ModelService modelService;

    /**
     * 获取所有模型列表
     */
    public List<Model> findAll() {
        log.debug("UseCase: find all models");
        return modelService.findAll();
    }

    /**
     * 根据 ID 获取模型
     */
    public Optional<Model> findById(Long id) {
        log.debug("UseCase: find model by id={}", id);
        return modelService.findById(id);
    }

    /**
     * 根据模型编码获取模型
     */
    public Optional<Model> findByModelCode(String modelCode) {
        log.debug("UseCase: find model by code={}", modelCode);
        return modelService.findByModelCode(modelCode);
    }

    /**
     * 根据提供商 ID 获取模型列表
     */
    public List<Model> findByProviderId(Long providerId) {
        log.debug("UseCase: find models by providerId={}", providerId);
        return modelService.findByProviderId(providerId);
    }

    /**
     * 创建模型
     */
    public Model create(Model model) {
        log.info("UseCase: create model, displayName={}", model.getDisplayName());
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