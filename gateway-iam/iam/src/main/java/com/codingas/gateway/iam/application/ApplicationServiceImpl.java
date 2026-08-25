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
package com.codingas.gateway.iam.application;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.auth.RolePermissions;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.iam.apikey.UserApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用应用服务实现
 *
 * <p>管理应用聚合根的 CRUD 与渠道授权绑定。</p>
 * <p>code 全局唯一校验；创建时状态默认 ACTIVE；删除时级联清理渠道授权关联。</p>
 *
 * <p>Task 8：移除 {@code bindResilienceProfile} 与 ResilienceProfileGateway 依赖；
 * {@code timeout} 通过 create/update 直接透传（承接原 ResilienceProfile.timeout）。</p>
 *
 * <p>Task 5：{@code failureStrategy} 通过 create/update 直接透传，未传时默认 FAIL_RETRY。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationChannelRepository applicationChannelRepository;
    private final UserApiKeyRepository userApiKeyRepository;

    @Override
    @Transactional
    public Application create(ApplicationCommand command) {
        // code 全局唯一校验
        if (applicationRepository.findByCode(command.code()) != null) {
            throw new GatewayRequestException("APPLICATION_CODE_DUPLICATE",
                    "应用编码已存在: " + command.code());
        }

        Application app = new Application();
        app.setCode(command.code());
        app.setName(command.name());
        app.setDescription(command.description());
        // 透传应用级超时（0 表示用渠道默认，承接原 ResilienceProfile.timeout）
        app.setTimeout(command.timeout());
        // 透传应用级失败处理策略，未传时默认 FAIL_RETRY
        app.setFailureStrategy(command.failureStrategy() != null
                ? command.failureStrategy() : FailureStrategy.FAIL_RETRY);
        // 创建时状态默认 ACTIVE
        app.setState(ApplicationState.ACTIVE);

        Application saved = applicationRepository.save(app);
        log.info("Created application: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Override
    @Transactional
    public Application update(Long id, ApplicationCommand command) {
        Application app = applicationRepository.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }

        // code 变更时校验新 code 不与其他应用冲突
        if (!app.getCode().equals(command.code())) {
            if (applicationRepository.findByCode(command.code()) != null) {
                throw new GatewayRequestException("APPLICATION_CODE_DUPLICATE",
                        "应用编码已存在: " + command.code());
            }
        }

        app.setCode(command.code());
        app.setName(command.name());
        app.setDescription(command.description());
        // 透传应用级超时（0 表示用渠道默认，承接原 ResilienceProfile.timeout）
        app.setTimeout(command.timeout());
        // 透传应用级失败处理策略，未传时默认 FAIL_RETRY
        app.setFailureStrategy(command.failureStrategy() != null
                ? command.failureStrategy() : FailureStrategy.FAIL_RETRY);

        Application saved = applicationRepository.save(app);
        log.info("Updated application: id={}", saved.getId());
        return saved;
    }

    @Override
    public Application getById(Long id) {
        Application app = applicationRepository.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }
        return app;
    }

    @Override
    public List<Application> getAll() {
        return applicationRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 前置校验：应用下有 API Key 引用时拒绝删除，避免悬空引用
        if (!userApiKeyRepository.findByApplicationId(id).isEmpty()) {
            throw new GatewayRequestException("APPLICATION_HAS_API_KEYS",
                    "应用下还有 API Key，请先转移或删除");
        }
        // 级联清理渠道授权关联，避免孤儿数据
        applicationChannelRepository.deleteByApplicationId(id);
        applicationRepository.deleteById(id);
        log.info("Deleted application: id={}", id);
    }

    @Override
    public List<ApplicationChannel> listChannels(Long id) {
        // 渠道绑定属管理数据：拦截器按 GET /applications/** 放行 USER 后，这里兜底仅管理员可查
        assertAdmin();
        // 经 gateway 取含 priority 的关联列表（priority 原样透传，null 表示未配置）
        return applicationChannelRepository.findByApplicationId(id);
    }

    /** 管理端操作校验：仅管理员可执行 */
    private void assertAdmin() {
        if (!StpUtil.hasRole(RolePermissions.ROLE_ADMIN)) {
            throw new ForbiddenException("仅管理员可执行该操作");
        }
    }

    @Override
    @Transactional
    public void updateChannels(Long id, List<ApplicationChannelCommand> channels) {
        Application app = applicationRepository.findById(id);
        if (app == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id);
        }

        // 先删后建：清空旧关联，再用三参构造器透传 priority 批量保存新关联
        applicationChannelRepository.deleteByApplicationId(id);
        if (channels != null && !channels.isEmpty()) {
            List<ApplicationChannel> rels = channels.stream()
                    .map(item -> new ApplicationChannel(id, item.channelId(), item.priority()))
                    .toList();
            applicationChannelRepository.saveAll(rels);
        }
        log.info("Updated application channels: appId={}, count={}", id,
                channels != null ? channels.size() : 0);
    }
}
