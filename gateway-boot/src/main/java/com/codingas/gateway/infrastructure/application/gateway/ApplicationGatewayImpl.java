package com.codingas.gateway.infrastructure.application.gateway;

import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.infrastructure.application.gateway.database.dataobject.ApplicationDo;
import com.codingas.gateway.infrastructure.application.gateway.database.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用领域网关实现
 *
 * <p>负责 {@link Application} 与 {@link ApplicationDo} 的互转。
 * 审计字段（createdAt/updatedAt/createdBy/updatedBy）由
 * {@link com.codingas.gateway.infrastructure.common.BaseDo} 的
 * AuditingEntityListener 自动填充，转换时仅需透传。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationGatewayImpl implements ApplicationGateway {

    private final ApplicationRepository repository;

    @Override
    public Application findById(Long id) {
        return repository.findById(id).map(this::toEntity).orElse(null);
    }

    @Override
    public Application findByCode(String code) {
        return repository.findByCode(code).map(this::toEntity).orElse(null);
    }

    @Override
    public List<Application> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public Application save(Application app) {
        ApplicationDo dataObject = toDataObject(app);
        ApplicationDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Application toEntity(ApplicationDo d) {
        Application entity = new Application();
        entity.setId(d.getId());
        entity.setCode(d.getCode());
        entity.setName(d.getName());
        entity.setDescription(d.getDescription());
        // 状态以字符串存储，读取时还原为枚举
        entity.setState(d.getState() != null ? ApplicationState.valueOf(d.getState()) : null);
        entity.setTimeout(d.getTimeout());
        entity.setFailureStrategy(d.getFailureStrategy());
        entity.setQuotaBudgetId(d.getQuotaBudgetId());
        entity.setDashboardId(d.getDashboardId());
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private ApplicationDo toDataObject(Application entity) {
        ApplicationDo d = new ApplicationDo();
        d.setId(entity.getId());
        d.setCode(entity.getCode());
        d.setName(entity.getName());
        d.setDescription(entity.getDescription());
        // 状态缺省 ACTIVE，保证 NOT NULL 约束
        d.setState(entity.getState() != null ? entity.getState().name() : ApplicationState.ACTIVE.name());
        d.setTimeout(entity.getTimeout());
        // failureStrategy 缺省 FAIL_RETRY，保证 NOT NULL 约束
        d.setFailureStrategy(entity.getFailureStrategy() != null ? entity.getFailureStrategy() : FailureStrategy.FAIL_RETRY);
        d.setQuotaBudgetId(entity.getQuotaBudgetId());
        d.setDashboardId(entity.getDashboardId());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
