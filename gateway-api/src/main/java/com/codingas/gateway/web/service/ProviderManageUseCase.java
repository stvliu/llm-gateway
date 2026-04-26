package com.codingas.gateway.web.service;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 提供商管理用例编排器
 *
 * <p>Application 层用例编排器，负责 Provider 的 CRUD 操作。
 * <p>遵循 COLA 5.0 架构：接收 Controller 的 DTO，编排 Domain Service，返回响应 DTO。
 *
 * <p>职责：
 * <ul>
 *   <li>编排 ProviderService 实现 CRUD 操作</li>
 *   <li>处理业务异常转换</li>
 *   <li>记录审计日志</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderManageUseCase {

    private final ProviderService providerService;

    /**
     * 获取所有提供商列表
     */
    public List<Provider> findAll() {
        log.debug("UseCase: find all providers");
        return providerService.findAll();
    }

    /**
     * 根据 ID 获取提供商
     */
    public Optional<Provider> findById(Long id) {
        log.debug("UseCase: find provider by id={}", id);
        return providerService.findById(id);
    }

    /**
     * 根据提供商编码获取提供商
     */
    public Optional<Provider> findByProviderCode(String providerCode) {
        log.debug("UseCase: find provider by code={}", providerCode);
        return providerService.findByProviderCode(providerCode);
    }

    /**
     * 根据状态获取提供商列表
     */
    public List<Provider> findByStatus(ProviderStatus status) {
        log.debug("UseCase: find providers by status={}", status);
        return providerService.findByStatus(status);
    }

    /**
     * 创建提供商
     */
    public Provider create(Provider provider) {
        log.info("UseCase: create provider, providerName={}", provider.getProviderName());
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