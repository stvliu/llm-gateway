package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.codingas.gateway.application.catalog.CatalogMaterializeService;
import com.codingas.gateway.application.catalog.CatalogService;
import com.codingas.gateway.application.catalog.CatalogSyncService;
import com.codingas.gateway.application.catalog.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录管理 REST 控制器
 *
 * <p>提供供应商目录、套餐目录、模型规格目录的查询、物化、同步 API。</p>
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
     * @param providerType 供应商类型过滤（可选）
     * @param keyword 关键词过滤（可选）
     * @return 供应商目录列表
     */
    @GetMapping("/providers")
    public ResponseEntity<List<ProviderCatalogResponse>> listProviders(
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(catalogService.listProviderCatalogs(providerType, keyword));
    }

    // ===== 套餐目录 =====

    /**
     * 列出套餐目录
     *
     * @param providerCode 供应商编码过滤（可选）
     * @return 套餐目录列表
     */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanCatalogResponse>> listPlans(
            @RequestParam(required = false) String providerCode) {
        return ResponseEntity.ok(catalogService.listPlanCatalogs(providerCode));
    }

    /**
     * 获取套餐详情
     *
     * @param planCode 套餐编码
     * @return 套餐详情
     */
    @GetMapping("/plans/{planCode}")
    public ResponseEntity<PlanDetailResponse> getPlanDetail(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogService.getPlanDetail(planCode));
    }

    // ===== 模型规格目录 =====

    /**
     * 列出模型规格目录
     *
     * @param keyword 关键词过滤（可选）
     * @param capability 能力标签过滤（可选）
     * @return 模型规格目录列表
     */
    @GetMapping("/model-specs")
    public ResponseEntity<List<ModelSpecCatalogResponse>> listModelSpecs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String capability) {
        return ResponseEntity.ok(catalogService.listModelSpecCatalogs(null, keyword, capability));
    }

    // ===== 物化（管理操作） =====

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
    public ResponseEntity<MaterializeResult> materializeProvider(@PathVariable String providerCode) {
        return ResponseEntity.ok(catalogMaterializeService.materializeProvider(providerCode));
    }

    /**
     * 物化套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ChannelModel 运营实体。</p>
     *
     * @param planCode 套餐编码
     * @return 物化结果
     */
    @PostMapping("/materialize/plan/{planCode}")
    @SaCheckRole("ADMIN")
    public ResponseEntity<MaterializeResult> materializePlan(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogMaterializeService.materializePlan(planCode));
    }

    /**
     * 物化模型规格
     *
     * <p>从 ModelSpecCatalog 创建 ModelSpec 运营实体。</p>
     *
     * @param providerModelId 供应商模型标识
     * @return 物化结果
     */
    @PostMapping("/materialize/model-spec/{providerModelId}")
    @SaCheckRole("ADMIN")
    public ResponseEntity<MaterializeResult> materializeModelSpec(@PathVariable String providerModelId) {
        return ResponseEntity.ok(catalogMaterializeService.materializeModelSpec(providerModelId));
    }

    // ===== 同步（管理操作） =====

    /**
     * 同步 BUILTIN 目录数据
     *
     * <p>强制重新加载 BUILTIN 目录数据，upsert 规则保证已有记录不会被重复创建。</p>
     */
    @PostMapping("/sync/builtin")
    @SaCheckRole("ADMIN")
    public ResponseEntity<Void> syncBuiltin() {
        catalogSyncService.syncBuiltin();
        return ResponseEntity.ok().build();
    }

    /**
     * 同步 Models.dev 数据
     */
    @PostMapping("/sync/models-dev")
    @SaCheckRole("ADMIN")
    public ResponseEntity<Void> syncModelsDev() {
        catalogSyncService.syncModelsDev();
        return ResponseEntity.ok().build();
    }
}
