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

import com.codingas.gateway.iam.application.ApplicationChannelConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理控制器
 *
 * <p>运维手工失效入口：按应用失效（applicationId）或清空全部缓存（缺省）。
 * 管理路径授权由拦截器链保证（未匹配授权规则 → 仅 ADMIN）。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final ApplicationChannelConfigProvider applicationChannelConfigProvider;

    /**
     * 缓存失效（手工运维）
     *
     * @param applicationId 应用 ID（可空——为空时清空全部缓存）
     */
    @PostMapping("/evict")
    public void evict(@RequestParam(value = "applicationId", required = false) Long applicationId) {
        if (applicationId != null) {
            applicationChannelConfigProvider.evict(applicationId);
        } else {
            applicationChannelConfigProvider.clearAll();
        }
    }
}
