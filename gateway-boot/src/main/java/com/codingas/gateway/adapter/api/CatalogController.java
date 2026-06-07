package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.codingas.gateway.application.catalog.CatalogSyncFacade;
import com.codingas.gateway.application.catalog.ChannelProvisionService;
import com.codingas.gateway.application.catalog.PlanCatalogService;
import com.codingas.gateway.application.catalog.dto.BatchProvisionRequest;
import com.codingas.gateway.application.catalog.dto.BatchProvisionResult;
import com.codingas.gateway.application.catalog.dto.ModelResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;
import com.codingas.gateway.application.catalog.dto.ProvisionRequest;
import com.codingas.gateway.application.catalog.dto.ProvisionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录管理 REST 控制器
 *
 * <p>提供供应商目录、套餐目录、模型的查询、开通、同步 API。</p>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final PlanCatalogService planCatalogService;
    private final ChannelProvisionService channelProvisionService;
    private final CatalogSyncFacade catalogSyncFacade;

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
    @GetMapping("/plans")
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
    @GetMapping("/plans/{planCode}")
    public PlanDetailResponse getPlanDetail(@PathVariable String planCode) {
        return planCatalogService.getPlanDetail(planCode);
    }

    // ===== 模型 =====

    /**
     * 列出模型
     *
     * @param keyword 关键词过滤（可选）
     * @param capability 能力标签过滤（可选）
     * @return 模型列表
     */
    @GetMapping("/models")
    public List<ModelResponse> listModels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String capability) {
        return planCatalogService.listModels(null, keyword, capability);
    }

    // ===== 开通（管理操作） =====

    /**
     * 级联开通供应商（含关联套餐）
     *
     * <p>开通 Provider 并级联创建所有（或指定）套餐的 Channel + Endpoint + ModelInstance。</p>
     *
     * @param providerCode 供应商编码
     * @param request      批量开通请求（可选 planCodes）
     * @return 批量开通结果
     */
    @PostMapping("/provision/provider/{providerCode}/with-plans")
    @SaCheckRole("ADMIN")
    public BatchProvisionResult provisionProviderWithPlans(
            @PathVariable String providerCode,
            @RequestBody(required = false) BatchProvisionRequest request) {
        return channelProvisionService.provisionBatch(providerCode, request);
    }

    /**
     * 开通套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance 运营实体。</p>
     * <p>支持通过 request 批量创建 API Key 凭证。</p>
     *
     * @param planCode 套餐编码
     * @param request  扩展请求（可选：apiKeys）
     * @return 开通结果
     */
    @PostMapping("/provision/plan/{planCode}")
    @SaCheckRole("ADMIN")
    public ProvisionResult provisionPlan(
            @PathVariable String planCode,
            @RequestBody(required = false) ProvisionRequest request) {
        return channelProvisionService.provisionFromPlan(planCode, request);
    }

    /**
     * 开通模型
     *
     * <p>创建 Model 运营实体。</p>
     *
     * @param modelName 模型名称
     * @return 开通结果
     */
    @PostMapping("/provision/model/{modelName}")
    @SaCheckRole("ADMIN")
    public ProvisionResult provisionModel(@PathVariable String modelName) {
        return channelProvisionService.provisionModel(modelName);
    }

    // ===== 同步（管理操作） =====

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