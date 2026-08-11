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
package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.codingas.gateway.application.catalog.CatalogSyncFacade;
import com.codingas.gateway.application.catalog.ChannelProvisionService;
import com.codingas.gateway.application.catalog.dto.BatchProvisionRequest;
import com.codingas.gateway.application.catalog.dto.BatchProvisionResult;
import com.codingas.gateway.application.catalog.dto.ProvisionRequest;
import com.codingas.gateway.application.catalog.dto.ProvisionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 渠道开通 REST 控制器
 *
 * <p>提供从套餐目录开通渠道、批量开通供应商、同步目录数据的 API。</p>
 * <p>从 CatalogController 拆分而来，专注于开通（运维）功能。</p>
 * <p>所有写入操作需要 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/api/v1/provision")
@RequiredArgsConstructor
public class ChannelProvisionController {

    private final ChannelProvisionService channelProvisionService;
    private final CatalogSyncFacade catalogSyncFacade;

    // ===== 开通操作（需要 ADMIN 权限） =====

    /**
     * 从套餐创建渠道
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance 运营实体。</p>
     * <p>支持通过 request 批量创建 API Key 凭证。</p>
     *
     * @param planCode 套餐编码
     * @param request  扩展请求（可选：apiKeys）
     * @return 开通结果
     */
    @PostMapping("/from-plan/{planCode}")
    @SaCheckRole("ADMIN")
    public ProvisionResult provisionFromPlan(
            @PathVariable String planCode,
            @RequestBody(required = false) ProvisionRequest request) {
        return channelProvisionService.provisionFromPlan(planCode, request);
    }

    /**
     * 批量开通供应商
     *
     * <p>开通 Provider 并级联创建所有（或指定）套餐的 Channel + Endpoint + ModelInstance。</p>
     *
     * @param providerCode 供应商编码
     * @param request      批量开通请求（可选 planCodes）
     * @return 批量开通结果
     */
    @PostMapping("/batch/{providerCode}")
    @SaCheckRole("ADMIN")
    public BatchProvisionResult provisionBatch(
            @PathVariable String providerCode,
            @RequestBody(required = false) BatchProvisionRequest request) {
        return channelProvisionService.provisionBatch(providerCode, request);
    }

    /**
     * 开通模型
     *
     * <p>创建 Model 运营实体。</p>
     *
     * @param modelName 模型名称
     * @return 开通结果
     */
    @PostMapping("/model/{modelName}")
    @SaCheckRole("ADMIN")
    public ProvisionResult provisionModel(@PathVariable String modelName) {
        return channelProvisionService.provisionModel(modelName);
    }

    // ===== 同步操作（需要 ADMIN 权限） =====

    /**
     * 同步 BUILTIN 目录数据
     *
     * <p>强制重新加载 BUILTIN 目录数据，upsert 规则保证已有记录不会被重复创建。</p>
     */
    @PostMapping("/sync/builtin")
    @SaCheckRole("ADMIN")
    public void syncBuiltin() {
        catalogSyncFacade.syncBuiltin();
    }
}