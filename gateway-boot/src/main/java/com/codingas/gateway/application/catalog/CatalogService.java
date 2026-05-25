package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.ModelSpecCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;

import java.util.List;

/**
 * 目录查询服务接口
 */
public interface CatalogService {

    /**
     * 列出供应商目录
     *
     * @param providerType 供应商类型过滤（可选）
     * @param keyword 关键词过滤（可选）
     * @return 供应商目录列表
     */
    List<ProviderCatalogResponse> listProviderCatalogs(String providerType, String keyword);

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
     * 列出模型规格目录
     *
     * @param providerCode 供应商编码过滤（可选）
     * @param keyword 关键词过滤（可选）
     * @param capability 能力标签过滤（可选）
     * @return 模型规格目录列表
     */
    List<ModelSpecCatalogResponse> listModelSpecCatalogs(String providerCode, String keyword, String capability);
}
