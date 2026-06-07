package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.codingas.gateway.application.catalog.CatalogMaterializeService;
import com.codingas.gateway.application.catalog.CatalogService;
import com.codingas.gateway.application.catalog.CatalogSyncService;
import com.codingas.gateway.application.catalog.dto.MaterializeBatchRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeBatchResult;
import com.codingas.gateway.application.catalog.dto.MaterializePlanRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeResult;
import com.codingas.gateway.application.catalog.dto.ModelResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录管理 REST 控制器
 *
 * <p>提供供应商目录、套餐目录、模型的查询、物化、同步 API。</p>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final CatalogMaterializeService catalogMaterializeService;
    private final CatalogSyncService catalogSyncService;

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
        return catalogService.listProviderCatalogs(keyword);
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
        return catalogService.listPlanCatalogs(providerCode);
    }

    /**
     * 获取套餐详情
     *
     * @param planCode 套餐编码
     * @return 套餐详情
     */
    @GetMapping("/plans/{planCode}")
    public PlanDetailResponse getPlanDetail(@PathVariable String planCode) {
        return catalogService.getPlanDetail(planCode);
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
        return catalogService.listModels(null, keyword, capability);
    }

    // ===== 物化（管理操作） =====

    /**
     * 级联物化供应商（含关联 Plans）
     *
     * <p>物化 Provider 并级联创建所有（或指定）Plans 的 Channel + Endpoint + Model。</p>
     *
     * @param providerCode 供应商编码
     * @param request      批量物化请求（可选 planCodes）
     * @return 批量物化结果
     */
    @PostMapping("/materialize/provider/{providerCode}/with-plans")
    @SaCheckRole("ADMIN")
    public MaterializeBatchResult materializeProviderWithPlans(
            @PathVariable String providerCode,
            @RequestBody(required = false) MaterializeBatchRequest request) {
        return catalogMaterializeService.materializeProviderWithPlans(providerCode, request);
    }

    /**
     * 物化供应商
     *
     * <p>从 ProviderCatalog 创建 Provider 运营实体。</p>
     *
     * @param providerCode 供应商编码
     * @return 物化结果
     */
    @PostMapping("/materialize/provider/{providerCode}")
    @SaCheckRole("ADMIN")
    public MaterializeResult materializeProvider(@PathVariable String providerCode) {
        return catalogMaterializeService.materializeProvider(providerCode);
    }

    /**
     * 物化套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ChannelModel 运营实体。</p>
     * <p>支持通过 request 批量创建 API Key 凭证。</p>
     *
     * @param planCode 套餐编码
     * @param request  扩展请求（可选：apiKeys / endpoints / models）
     * @return 物化结果
     */
    @PostMapping("/materialize/plan/{planCode}")
    @SaCheckRole("ADMIN")
    public MaterializeResult materializePlan(
            @PathVariable String planCode,
            @RequestBody(required = false) MaterializePlanRequest request) {
        if (request != null && (request.getApiKeys() != null || request.getEndpoints() != null || request.getModels() != null)) {
            return catalogMaterializeService.materializePlan(planCode, request);
        }
        return catalogMaterializeService.materializePlan(planCode);
    }

    /**
     * 物化模型
     *
     * <p>创建 Model 运营实体。</p>
     *
     * @param modelName 模型名称
     * @return 物化结果
     */
    @PostMapping("/materialize/model/{modelName}")
    @SaCheckRole("ADMIN")
    public MaterializeResult materializeModel(@PathVariable String modelName) {
        return catalogMaterializeService.materializeModel(modelName);
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
        catalogSyncService.syncBuiltin();
    }

    /**
     * 同步 Models.dev 数据
     */
    @PostMapping("/sync/models-dev")
    @SaCheckRole("ADMIN")
    public void syncModelsDev() {
        catalogSyncService.syncModelsDev();
    }
}
