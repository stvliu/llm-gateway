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
package com.codingas.gateway.web.api;

import com.codingas.gateway.web.api.dto.ApplicationChannelRequest;
import com.codingas.gateway.iam.application.ApplicationManager;
import com.codingas.gateway.iam.apikey.UserApiKeyManager;
import com.codingas.gateway.web.api.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理 REST 控制器
 *
 * <p>提供应用根实体的 CRUD 与渠道授权绑定 API。
 * Application 是权限+行为双根实体，承载 Key 归属与渠道可见性。</p>
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationManager applicationManager;
    private final UserApiKeyManager userApiKeyManager;

    /**
     * 创建应用
     *
     * @param request 创建请求（code/name/description）
     * @return 创建后的应用响应
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody ApplicationRequest request) {
        return ApplicationResponse.from(applicationManager.create(request.toEntity()));
    }

    /**
     * 更新应用
     *
     * @param id      应用 ID
     * @param request 更新请求
     * @return 更新后的应用响应
     */
    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest request) {
        return ApplicationResponse.from(applicationManager.update(id, request.toEntity()));
    }

    /**
     * 查询应用详情
     *
     * @param id 应用 ID
     * @return 应用响应
     */
    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable Long id) {
        return ApplicationResponse.from(applicationManager.getById(id));
    }

    /**
     * 查询全部应用列表
     *
     * @return 应用响应列表
     */
    @GetMapping
    public List<ApplicationResponse> list() {
        return ApplicationResponse.from(applicationManager.getAll());
    }

    /**
     * 删除应用（级联清理渠道授权关联）
     *
     * @param id 应用 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationManager.delete(id);
    }

    /**
     * 查询应用下的所有 API Key
     *
     * @param id 应用 ID
     * @return 该应用下的 API Key 响应列表
     */
    @GetMapping("/{id}/api-keys")
    public List<UserApiKeyResponse> listApiKeys(@PathVariable Long id) {
        return UserApiKeyResponse.from(userApiKeyManager.findByApplicationId(id));
    }

    /**
     * 查询应用授权的渠道及其应用级转移优先级
     *
     * @param id 应用 ID
     * @return 渠道授权项列表（channelId + priority）
     */
    @GetMapping("/{id}/channels")
    public List<ApplicationChannelItem> listChannels(@PathVariable Long id) {
        return ApplicationChannelItem.from(applicationManager.listChannels(id));
    }

    /**
     * 更新应用渠道授权（先清空旧关联，再批量保存含 priority 的新关联）
     *
     * @param id      应用 ID
     * @param request 渠道授权请求（channels，每项含 channelId 与 priority）
     */
    @PutMapping("/{id}/channels")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateChannels(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationChannelRequest request) {
        applicationManager.updateChannels(id,
                request.channels().stream().map(ApplicationChannelItem::toEntity).toList());
    }
}
