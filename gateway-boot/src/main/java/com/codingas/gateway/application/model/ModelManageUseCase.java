package com.codingas.gateway.application.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型管理用例编排器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManageUseCase {

    public List<Object> findAll() {
        log.debug("UseCase: find all models");
        throw new UnsupportedOperationException("ModelService not yet available");
    }

    public Object findById(Long id) {
        log.debug("UseCase: find model by id={}", id);
        throw new UnsupportedOperationException("ModelService not yet available");
    }

    public Object create(Object model) {
        log.info("UseCase: create model");
        throw new UnsupportedOperationException("ModelService not yet available");
    }

    public Object update(Long id, Object model) {
        log.info("UseCase: update model, id={}", id);
        throw new UnsupportedOperationException("ModelService not yet available");
    }

    public void delete(Long id) {
        log.info("UseCase: delete model, id={}", id);
        throw new UnsupportedOperationException("ModelService not yet available");
    }
}
