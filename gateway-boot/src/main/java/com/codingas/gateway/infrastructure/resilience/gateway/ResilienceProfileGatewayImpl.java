package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ResilienceProfileDo;
import com.codingas.gateway.infrastructure.resilience.gateway.database.repository.ResilienceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 容灾画像领域网关实现
 *
 * <p>负责 {@link ResilienceProfile} 与 {@link ResilienceProfileDo} 的互转。
 * 审计字段（createdAt/updatedAt/createdBy/updatedBy）由
 * {@link com.codingas.gateway.infrastructure.common.BaseDo} 的
 * AuditingEntityListener 自动填充，转换时仅需透传。</p>
 *
 * <p>mode 字段以字符串存储于 DO，读取时还原为 {@link ResilienceMode} 枚举，
 * 写入时取枚举名转字符串（参照 ApplicationGatewayImpl 的 state 字段处理模式）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilienceProfileGatewayImpl implements ResilienceProfileGateway {

    private final ResilienceProfileRepository repository;

    @Override
    public ResilienceProfile findById(Long id) {
        return repository.findById(id).map(this::toEntity).orElse(null);
    }

    @Override
    public ResilienceProfile findByCode(String code) {
        return repository.findByCode(code).map(this::toEntity).orElse(null);
    }

    @Override
    public List<ResilienceProfile> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public ResilienceProfile save(ResilienceProfile profile) {
        ResilienceProfileDo dataObject = toDataObject(profile);
        ResilienceProfileDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    private ResilienceProfile toEntity(ResilienceProfileDo d) {
        ResilienceProfile entity = new ResilienceProfile();
        entity.setId(d.getId());
        entity.setCode(d.getCode());
        entity.setName(d.getName());
        // mode 以字符串存储，读取时还原为枚举
        entity.setMode(d.getMode() != null ? ResilienceMode.valueOf(d.getMode()) : null);
        entity.setEnableL2ModelDegradation(d.isEnableL2ModelDegradation());
        entity.setDegradationMaxDepth(d.getDegradationMaxDepth());
        entity.setTimeout(d.getTimeout());
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private ResilienceProfileDo toDataObject(ResilienceProfile entity) {
        ResilienceProfileDo d = new ResilienceProfileDo();
        d.setId(entity.getId());
        d.setCode(entity.getCode());
        d.setName(entity.getName());
        // 枚举转字符串存储
        d.setMode(entity.getMode() != null ? entity.getMode().name() : null);
        d.setEnableL2ModelDegradation(entity.isEnableL2ModelDegradation());
        d.setDegradationMaxDepth(entity.getDegradationMaxDepth());
        d.setTimeout(entity.getTimeout());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
