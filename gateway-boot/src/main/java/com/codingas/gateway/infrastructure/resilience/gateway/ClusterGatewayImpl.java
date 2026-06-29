package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus;
import com.codingas.gateway.domain.resilience.gateway.ClusterGateway;
import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ClusterDo;
import com.codingas.gateway.infrastructure.resilience.gateway.database.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cluster 故障域领域网关实现
 *
 * <p>负责 {@link Cluster} 与 {@link ClusterDo} 的互转。
 * 审计字段（createdAt/updatedAt/createdBy/updatedBy）由
 * {@link com.codingas.gateway.infrastructure.common.BaseDo} 的
 * AuditingEntityListener 自动填充，转换时仅需透传。</p>
 *
 * <p>healthStatus 字段以字符串存储于 DO，读取时还原为 {@link ClusterHealthStatus} 枚举，
 * 写入时取枚举名转字符串（参照 ResilienceProfileGatewayImpl 的 mode 字段处理模式）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterGatewayImpl implements ClusterGateway {

    private final ClusterRepository repository;

    @Override
    public Cluster findById(Long id) {
        return repository.findById(id).map(this::toEntity).orElse(null);
    }

    @Override
    public Cluster findByCode(String code) {
        return repository.findByCode(code).map(this::toEntity).orElse(null);
    }

    @Override
    public List<Cluster> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public Cluster save(Cluster cluster) {
        ClusterDo dataObject = toDataObject(cluster);
        ClusterDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    private Cluster toEntity(ClusterDo d) {
        Cluster entity = new Cluster();
        entity.setId(d.getId());
        entity.setCode(d.getCode());
        entity.setName(d.getName());
        entity.setProviderId(d.getProviderId());
        entity.setRegion(d.getRegion());
        entity.setPriority(d.getPriority());
        // healthStatus 以字符串存储，读取时还原为枚举
        entity.setHealthStatus(d.getHealthStatus() != null ? ClusterHealthStatus.valueOf(d.getHealthStatus()) : null);
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private ClusterDo toDataObject(Cluster entity) {
        ClusterDo d = new ClusterDo();
        d.setId(entity.getId());
        d.setCode(entity.getCode());
        d.setName(entity.getName());
        d.setProviderId(entity.getProviderId());
        d.setRegion(entity.getRegion());
        d.setPriority(entity.getPriority());
        // 枚举转字符串存储
        d.setHealthStatus(entity.getHealthStatus() != null ? entity.getHealthStatus().name() : null);
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
