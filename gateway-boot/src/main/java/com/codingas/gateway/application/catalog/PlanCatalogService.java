package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.ModelResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;

import java.util.List;

/**
 * 套餐目录查询服务接口
 *
 * <p>提供套餐目录、供应商目录、模型的查询功能。</p>
 * <p>替代原 CatalogService 的查询功能。</p>
 */
public interface PlanCatalogService {

    /**
     * 列出供应商目录
     *
     * @param keyword 关键词过滤（可选）
     * @return 供应商目录列表
     */
    List<ProviderCatalogResponse> listProviderCatalogs(String keyword);

    /**
     * 列出套餐目录
     *
     * @param providerCode 供应商编码过滤（可选）
     * @return 套餐目录列表
     */
    List<PlanCatalogResponse> listPlanCatalogs(String providerCode);

    /**
     * 获取套餐详情
     *
     * @param planCode 套餐编码
     * @return 套餐详情
     */
    PlanDetailResponse getPlanDetail(String planCode);

    /**
     * 获取套餐定价列表
     *
     * @param planCode 套餐编码
     * @return 定价列表
     */
    List<PlanDetailResponse.PricingInfo> getPricing(String planCode);

    /**
     * 列出模型
     *
     * @param providerCode 供应商编码过滤（可选）
     * @param keyword      关键词过滤（可选）
     * @param capability   能力标签过滤（可选）
     * @return 模型列表
     */
    List<ModelResponse> listModels(String providerCode, String keyword, String capability);
}