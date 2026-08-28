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

import com.codingas.gateway.settings.SystemSettingService;
import com.codingas.gateway.web.api.dto.SettingUpdateRequest;
import com.codingas.gateway.web.api.dto.SystemSettingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统设置控制器
 *
 * <p>提供系统全局配置项的查询与更新端点：GET 返回全部配置，
 * PUT 按 key 更新配置值（参数非法由 GlobalExceptionHandler 映射为 400）。</p>
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemSettingService settingService;

    /**
     * 查询全部系统配置
     *
     * @return 全部配置项列表
     */
    @GetMapping
    public List<SystemSettingResponse> getAll() {
        return settingService.getAll().stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    /**
     * 更新指定配置项
     *
     * @param key     设置键
     * @param request 更新请求（携带新值）
     * @return 更新后的配置项
     */
    @PutMapping("/{key}")
    public SystemSettingResponse update(@PathVariable String key,
                                        @Valid @RequestBody SettingUpdateRequest request) {
        return SystemSettingResponse.from(settingService.update(key, request.getValue()));
    }
}
