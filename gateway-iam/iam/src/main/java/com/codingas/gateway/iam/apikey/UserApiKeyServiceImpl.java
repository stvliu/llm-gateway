/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.apikey;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.iam.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.iam.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.iam.dto.UserApiKeyResponse;
import com.codingas.gateway.iam.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.iam.auth.RolePermissions;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.iam.application.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 API Key 应用服务实现
 *
 * <p>applicationId 为权限锚点：create 必填并校验 Application 存在；
 * update 支持补绑/转移（非 null 时校验存在）。</p>
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserApiKeyServiceImpl.class);

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserApiKeyGenerator userApiKeyGenerator;
    private final ApplicationRepository applicationRepository;

    public UserApiKeyServiceImpl(UserApiKeyRepository userApiKeyRepository,
                                 UserApiKeyGenerator userApiKeyGenerator,
                                 ApplicationRepository applicationRepository) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.userApiKeyGenerator = userApiKeyGenerator;
        this.applicationRepository = applicationRepository;
    }

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        // 校验 Application 存在（applicationId 为权限锚点，引用必须有效）
        validateApplicationExists(request.applicationId());

        GeneratedApiKey generated = userApiKeyGenerator.generate();

        UserApiKey apiKey = new UserApiKey();
        // 数据归属：普通用户强制归属当前登录用户（忽略请求体 userId，防止越权代建）
        apiKey.setUserId(isAdmin() ? request.userId() : currentUserId());
        apiKey.setApplicationId(request.applicationId());
        apiKey.setKeyPrefix(generated.keyPrefix());
        apiKey.setKeyPlain(generated.plainKey());
        apiKey.setName(request.name());

        UserApiKey saved = userApiKeyRepository.save(apiKey);
        log.info("Created UserApiKey: id={}, userId={}, applicationId={}",
                saved.getId(), saved.getUserId(), saved.getApplicationId());

        return new UserApiKeyCreateResponse(saved.getId(), generated.keyPrefix(), generated.plainKey());
    }

    @Override
    public List<UserApiKeyResponse> findByUserId(Long userId) {
        // 数据范围：普通用户只能查询自己的 Key（MeController/Quickstart 均传自身 userId）
        assertAccessToUser(userId);
        return userApiKeyRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findAllNonDeleted() {
        // 全部 Key 列表为管理操作，仅管理员可查
        assertAdmin();
        return userApiKeyRepository.findAllNonDeleted().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findByApplicationId(Long applicationId) {
        // 按应用查询为管理操作，仅管理员可查
        assertAdmin();
        return userApiKeyRepository.findByApplicationId(applicationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserApiKeyResponse getById(Long id) {
        UserApiKey apiKey = userApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        assertOwned(apiKey);
        return toResponse(apiKey);
    }

    @Override
    public UserApiKeyDetailResponse getDetailById(Long id) {
        UserApiKey apiKey = userApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        assertOwned(apiKey);
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request) {
        UserApiKey apiKey = userApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        assertOwned(apiKey);

        // 补绑/转移 applicationId（非 null 时校验存在）
        if (request.applicationId() != null) {
            validateApplicationExists(request.applicationId());
            apiKey.setApplicationId(request.applicationId());
        }
        if (request.name() != null) {
            apiKey.setName(request.name());
        }

        UserApiKey saved = userApiKeyRepository.save(apiKey);
        log.info("Updated UserApiKey: id={}, applicationId={}", saved.getId(), saved.getApplicationId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserApiKey apiKey = userApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        assertOwned(apiKey);
        userApiKeyRepository.delete(apiKey);
        log.info("Deleted UserApiKey: id={}", id);
    }

    /** 当前登录用户 ID */
    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 是否管理员（授权基于 USER/ADMIN 两角色，角色由 StpRoleService 提供） */
    private boolean isAdmin() {
        return StpUtil.hasRole(RolePermissions.ROLE_ADMIN);
    }

    /**
     * 校验单条 Key 归属：非管理员只能操作自己的 Key
     *
     * @param apiKey 目标 Key
     * @throws ForbiddenException 普通用户操作他人 Key 时抛出
     */
    private void assertOwned(UserApiKey apiKey) {
        if (isAdmin()) {
            return;
        }
        if (!apiKey.getUserId().equals(currentUserId())) {
            throw new ForbiddenException("无权访问该 API Key");
        }
    }

    /**
     * 校验按用户查询的数据范围：非管理员只能查询自己的 Key
     *
     * @param userId 查询目标用户 ID
     * @throws ForbiddenException 普通用户查询他人 Key 时抛出
     */
    private void assertAccessToUser(Long userId) {
        if (isAdmin()) {
            return;
        }
        if (!userId.equals(currentUserId())) {
            throw new ForbiddenException("无权访问该用户的 API Key");
        }
    }

    /** 管理端操作校验：仅管理员可执行 */
    private void assertAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("仅管理员可执行该操作");
        }
    }

    /**
     * 校验 Application 存在
     *
     * <p>ApplicationRepository.findById 返回 null 表示不存在（沿用现有约定）。</p>
     *
     * @param applicationId 应用 ID
     * @throws GatewayRequestException 应用不存在时抛 APPLICATION_NOT_FOUND
     */
    private void validateApplicationExists(Long applicationId) {
        if (applicationRepository.findById(applicationId) == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + applicationId);
        }
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
        return new UserApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }
}
