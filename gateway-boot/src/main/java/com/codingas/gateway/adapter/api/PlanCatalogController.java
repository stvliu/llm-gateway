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

import com.codingas.gateway.application.catalog.PlanCatalogService;
import com.codingas.gateway.application.catalog.dto.ModelResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 套餐目录 REST 控制器
 *
 * <p>提供套餐目录、供应商目录、模型的查询 API。</p>
 * <p>从 CatalogController 拆分而来，专注于目录查询功能。</p>
 */
@RestController
@RequestMapping("/api/v1/plan-catalogs")
@RequiredArgsConstructor
public class PlanCatalogController {

    private final PlanCatalogService planCatalogService;

    // ===== 供应商目录 =====

    /**
     * 列出供应商目录
     *
     * @param keyword 关键词过滤（可选）
     * @return 供应商目录列表
     */
    @GetMapping("/providers")
    public List<ProviderCatalogResponse> listProviders(
            @RequestParam(required = false) String keyword) {
        return planCatalogService.listProviderCatalogs(keyword);
    }

    // ===== 套餐目录 =====

    /**
     * 列出套餐目录
     *
     * @param providerCode 供应商编码过滤（可选）
     * @return 套餐目录列表
     */
    @GetMapping
    public List<PlanCatalogResponse> listPlans(
            @RequestParam(required = false) String providerCode) {
        return planCatalogService.listPlanCatalogs(providerCode);
    }

    /**
     * 获取套餐详情
     *
     * @param planCode 套餐编码
     * @return 套餐详情
     */
    @GetMapping("/{planCode}")
    public PlanDetailResponse getPlanDetail(@PathVariable String planCode) {
        return planCatalogService.getPlanDetail(planCode);
    }

    /**
     * 获取套餐定价
     *
     * @param planCode 套餐编码
     * @return 定价列表
     */
    @GetMapping("/{planCode}/pricing")
    public List<PlanDetailResponse.PricingInfo> getPlanPricing(@PathVariable String planCode) {
        return planCatalogService.getPricing(planCode);
    }

    // ===== 模型 =====

    /**
     * 列出模型
     *
     * @param keyword    关键词过滤（可选）
     * @param capability 能力标签过滤（可选）
     * @return 模型列表
     */
    @GetMapping("/models")
    public List<ModelResponse> listModels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String capability) {
        return planCatalogService.listModels(null, keyword, capability);
    }
}