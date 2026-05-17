package com.codingas.gateway.application.gatewayapikey;

import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUsageResponse;
import com.codingas.gateway.common.dto.PageResponse;

import java.time.Instant;
import java.util.List;

/**
 * API Key 应用服务接口
 *
 * <p>处理 API Key 管理的业务逻辑。</p>
 */
public interface ApiKeyService {

    /**
     * 创建 API Key
     */
    ApiKeyResponse create(ApiKeyCreateRequest request);

    /**
     * 根据 ID 获取 API Key
     */
    ApiKeyResponse getById(Long id);

    /**
     * 查询 API Key 列表
     */
    PageResponse<ApiKeyResponse> query(ApiKeyQueryRequest request);

    /**
     * 更新 API Key
     */
    ApiKeyResponse update(Long id, ApiKeyUpdateRequest request);

    /**
     * 删除 API Key（软删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用 API Key
     */
    ApiKeyResponse setEnabled(Long id, boolean enabled);

    /**
     * 获取单个 API Key 的用量统计
     *
     * @param id        API Key ID
     * @param startDate 开始时间（可选，默认30天前）
     * @param endDate   结束时间（可选，默认当前时间）
     * @return 用量统计响应
     */
    ApiKeyUsageResponse getUsage(Long id, Instant startDate, Instant endDate);

    /**
     * 批量获取 API Key 的用量统计
     *
     * @param startDate 开始时间（可选，默认30天前）
     * @param endDate   结束时间（可选，默认当前时间）
     * @param userId    用户 ID 过滤（可选）
     * @return 用量统计响应列表
     */
    List<ApiKeyUsageResponse> getUsageBatch(Instant startDate, Instant endDate, Long userId);
}
