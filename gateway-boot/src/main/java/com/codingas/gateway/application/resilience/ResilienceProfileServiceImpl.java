package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ResilienceProfileRequest;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 容灾画像应用服务实现
 *
 * <p>管理容灾画像聚合根的 create/update/get/list。
 * 委托 {@link ResilienceProfileGateway}，code 全局唯一校验，mode 字符串还原为枚举。</p>
 *
 * <p>不提供 delete：default 画像为系统兜底禁删；其余画像因 Gateway 无 delete 方法遵循既有模式。
 * 如需删除画像，后续可在 Gateway 新增 delete 并加 default 画像保护，本服务暂不开放。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceProfileServiceImpl implements ResilienceProfileService {

    private final ResilienceProfileGateway resilienceProfileGateway;

    @Override
    @Transactional
    public ResilienceProfileResponse create(ResilienceProfileRequest request) {
        // code 全局唯一校验
        if (resilienceProfileGateway.findByCode(request.getCode()) != null) {
            throw new GatewayRequestException("RESILIENCE_PROFILE_CODE_DUPLICATE",
                    "容灾画像编码已存在: " + request.getCode());
        }

        ResilienceProfile profile = new ResilienceProfile();
        applyRequestToEntity(profile, request);

        ResilienceProfile saved = resilienceProfileGateway.save(profile);
        log.info("Created resilience profile: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ResilienceProfileResponse update(Long id, ResilienceProfileRequest request) {
        ResilienceProfile profile = resilienceProfileGateway.findById(id);
        if (profile == null) {
            throw new GatewayRequestException("RESILIENCE_PROFILE_NOT_FOUND",
                    "容灾画像不存在: " + id);
        }

        // code 变更时校验新 code 不与其他画像冲突
        if (!profile.getCode().equals(request.getCode())) {
            if (resilienceProfileGateway.findByCode(request.getCode()) != null) {
                throw new GatewayRequestException("RESILIENCE_PROFILE_CODE_DUPLICATE",
                        "容灾画像编码已存在: " + request.getCode());
            }
        }

        applyRequestToEntity(profile, request);

        ResilienceProfile saved = resilienceProfileGateway.save(profile);
        log.info("Updated resilience profile: id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public ResilienceProfileResponse getById(Long id) {
        ResilienceProfile profile = resilienceProfileGateway.findById(id);
        if (profile == null) {
            throw new GatewayRequestException("RESILIENCE_PROFILE_NOT_FOUND",
                    "容灾画像不存在: " + id);
        }
        return toResponse(profile);
    }

    @Override
    public List<ResilienceProfileResponse> getAll() {
        return resilienceProfileGateway.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将请求 DTO 字段应用到实体（mode 字符串还原为枚举）
     */
    private void applyRequestToEntity(ResilienceProfile profile, ResilienceProfileRequest request) {
        profile.setCode(request.getCode());
        profile.setName(request.getName());
        profile.setMode(ResilienceMode.valueOf(request.getMode()));
        profile.setEnableL2ModelDegradation(request.isEnableL2ModelDegradation());
        profile.setDegradationMaxDepth(request.getDegradationMaxDepth());
        profile.setEnableSessionAffinity(request.isEnableSessionAffinity());
        profile.setSessionAffinityTtlMinutes(request.getSessionAffinityTtlMinutes());
        profile.setEnablePinnedModel(request.isEnablePinnedModel());
        profile.setPinnedModelId(request.getPinnedModelId());
        profile.setTimeout(request.getTimeout());
    }

    /**
     * 实体转响应 DTO
     */
    private ResilienceProfileResponse toResponse(ResilienceProfile profile) {
        ResilienceProfileResponse response = new ResilienceProfileResponse();
        response.setId(profile.getId());
        response.setCode(profile.getCode());
        response.setName(profile.getName());
        response.setMode(profile.getMode() != null ? profile.getMode().name() : null);
        response.setEnableL2ModelDegradation(profile.isEnableL2ModelDegradation());
        response.setDegradationMaxDepth(profile.getDegradationMaxDepth());
        response.setEnableSessionAffinity(profile.isEnableSessionAffinity());
        response.setSessionAffinityTtlMinutes(profile.getSessionAffinityTtlMinutes());
        response.setEnablePinnedModel(profile.isEnablePinnedModel());
        response.setPinnedModelId(profile.getPinnedModelId());
        response.setTimeout(profile.getTimeout());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
