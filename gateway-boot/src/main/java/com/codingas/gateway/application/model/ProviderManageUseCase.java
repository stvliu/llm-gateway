package com.codingas.gateway.application.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 提供商管理用例编排器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderManageUseCase {

    public List<Object> findAll() {
        log.debug("UseCase: find all providers");
        throw new UnsupportedOperationException("ProviderService not yet available");
    }

    public Object findById(Long id) {
        log.debug("UseCase: find provider by id={}", id);
        throw new UnsupportedOperationException("ProviderService not yet available");
    }

    public Object create(Object provider) {
        log.info("UseCase: create provider");
        throw new UnsupportedOperationException("ProviderService not yet available");
    }

    public Object update(Long id, Object provider) {
        log.info("UseCase: update provider, id={}", id);
        throw new UnsupportedOperationException("ProviderService not yet available");
    }

    public void delete(Long id) {
        log.info("UseCase: delete provider, id={}", id);
        throw new UnsupportedOperationException("ProviderService not yet available");
    }
}
