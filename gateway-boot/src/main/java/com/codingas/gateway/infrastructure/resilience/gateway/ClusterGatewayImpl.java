package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.Cluster;
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
 * <p><b>Task 6 变更</b>：Cluster 字段瘦身为 code/name/description/providerId + 审计，
 * 删除 region/priority/healthStatus 转换（含域级健康状态枚举还原，该枚举随字段移除而退场）。</p>
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
        entity.setDescription(d.getDescription());
        entity.setProviderId(d.getProviderId());
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
        d.setDescription(entity.getDescription());
        d.setProviderId(entity.getProviderId());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
