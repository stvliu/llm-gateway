package com.codingas.gateway.application.model;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 提供商管理用例编排器
 *
 * <p>Application 层用例编排，负责提供商管理的请求处理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderManageUseCase {

    private final ProviderService providerService;

    /**
     * 查询所有提供商
     */
    public List<Provider> findAll() {
        log.debug("UseCase: find all providers");
        return providerService.findAll();
    }

    /**
     * 根据 ID 查询提供商
     */
    public Optional<Provider> findById(Long id) {
        log.debug("UseCase: find provider by id={}", id);
        return providerService.findById(id);
    }

    /**
     * 创建提供商
     */
    public Provider create(Provider provider) {
        log.info("UseCase: create provider, code={}", provider.getProviderCode());
        return providerService.create(provider);
    }

    /**
     * 更新提供商
     */
    public Provider update(Long id, Provider provider) {
        log.info("UseCase: update provider, id={}", id);
        return providerService.update(id, provider);
    }

    /**
     * 删除提供商
     */
    public void delete(Long id) {
        log.info("UseCase: delete provider, id={}", id);
        providerService.delete(id);
    }
}
