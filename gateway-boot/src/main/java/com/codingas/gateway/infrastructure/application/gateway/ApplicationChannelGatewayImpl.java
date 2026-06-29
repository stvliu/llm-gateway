package com.codingas.gateway.infrastructure.application.gateway;

import com.codingas.gateway.domain.application.entity.ApplicationChannel;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.infrastructure.application.gateway.database.dataobject.ApplicationChannelDo;
import com.codingas.gateway.infrastructure.application.gateway.database.repository.ApplicationChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 应用-渠道授权关联网关实现
 *
 * <p>负责 {@link ApplicationChannel} 与 {@link ApplicationChannelDo} 的互转；
 * 渠道 ID 集合查询结果去重后以 {@link Set} 返回。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationChannelGatewayImpl implements ApplicationChannelGateway {

    private final ApplicationChannelRepository repository;

    @Override
    public Set<Long> findChannelIdsByApplicationId(Long appId) {
        // Repository 返回 List（DISTINCT），此处转为 Set 保证去重语义
        List<Long> ids = repository.findChannelIdsByApplicationId(appId);
        return new LinkedHashSet<>(ids);
    }

    @Override
    public List<ApplicationChannel> findByApplicationId(Long appId) {
        return repository.findByApplicationId(appId).stream().map(this::toEntity).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<ApplicationChannel> rels) {
        List<ApplicationChannelDo> dataObjects = rels.stream().map(this::toDataObject).toList();
        repository.saveAll(dataObjects);
    }

    @Override
    public boolean existsByApplicationIdAndChannelId(Long appId, Long chId) {
        return repository.existsByApplicationIdAndChannelId(appId, chId);
    }

    @Override
    @Transactional
    public void deleteByApplicationId(Long appId) {
        repository.deleteByApplicationId(appId);
    }

    private ApplicationChannel toEntity(ApplicationChannelDo d) {
        ApplicationChannel entity = new ApplicationChannel();
        entity.setId(d.getId());
        entity.setApplicationId(d.getApplicationId());
        entity.setChannelId(d.getChannelId());
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private ApplicationChannelDo toDataObject(ApplicationChannel entity) {
        ApplicationChannelDo d = new ApplicationChannelDo();
        d.setId(entity.getId());
        d.setApplicationId(entity.getApplicationId());
        d.setChannelId(entity.getChannelId());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
