package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationChannel;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 应用应用服务实现
 *
 * <p>管理应用聚合根的 CRUD 与渠道授权绑定。</p>
 * <p>code 全局唯一校验；创建时状态默认 ACTIVE；删除时级联清理渠道授权关联。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationGateway applicationGateway;
    private final ApplicationChannelGateway applicationChannelGateway;

    @Override
    @Transactional
    public ApplicationResponse create(ApplicationRequest request) {
        // code 全局唯一校验
        if (applicationGateway.findByCode(request.getCode()) != null) {
            throw new GatewayRequestException("APPLICATION_CODE_DUPLICATE",
                    "应用编码已存在: " + request.getCode());
        }

        Application app = new Application();
        app.setCode(request.getCode());
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        // 创建时状态默认 ACTIVE
        app.setState(ApplicationState.ACTIVE);

        Application saved = applicationGateway.save(app);
        log.info("Created application: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse update(Long id, ApplicationRequest request) {
        Application app = applicationGateway.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }

        // code 变更时校验新 code 不与其他应用冲突
        if (!app.getCode().equals(request.getCode())) {
            if (applicationGateway.findByCode(request.getCode()) != null) {
                throw new GatewayRequestException("APPLICATION_CODE_DUPLICATE",
                        "应用编码已存在: " + request.getCode());
            }
        }

        app.setCode(request.getCode());
        app.setName(request.getName());
        app.setDescription(request.getDescription());

        Application saved = applicationGateway.save(app);
        log.info("Updated application: id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public ApplicationResponse getById(Long id) {
        Application app = applicationGateway.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }
        return toResponse(app);
    }

    @Override
    public List<ApplicationResponse> getAll() {
        return applicationGateway.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 级联清理渠道授权关联，避免孤儿数据
        applicationChannelGateway.deleteByApplicationId(id);
        applicationGateway.deleteById(id);
        log.info("Deleted application: id={}", id);
    }

    @Override
    public List<Long> listChannelIds(Long id) {
        Set<Long> channelIds = applicationChannelGateway.findChannelIdsByApplicationId(id);
        return new ArrayList<>(channelIds);
    }

    @Override
    @Transactional
    public void updateChannels(Long id, List<Long> channelIds) {
        Application app = applicationGateway.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }

        // 先删后建：清空旧关联，再批量保存新关联
        applicationChannelGateway.deleteByApplicationId(id);
        if (channelIds != null && !channelIds.isEmpty()) {
            List<ApplicationChannel> rels = channelIds.stream()
                    .map(chId -> new ApplicationChannel(id, chId))
                    .toList();
            applicationChannelGateway.saveAll(rels);
        }
        log.info("Updated application channels: appId={}, count={}", id,
                channelIds != null ? channelIds.size() : 0);
    }

    /**
     * 实体转响应 DTO
     */
    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getId());
        response.setCode(app.getCode());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setState(app.getState() != null ? app.getState().name() : null);
        response.setResilienceProfileId(app.getResilienceProfileId());
        response.setQuotaBudgetId(app.getQuotaBudgetId());
        response.setDashboardId(app.getDashboardId());
        response.setCreatedAt(app.getCreatedAt());
        response.setUpdatedAt(app.getUpdatedAt());
        return response;
    }
}
