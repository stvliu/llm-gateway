package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;

import java.util.List;

/**
 * 用户 API Key 应用服务
 */
public interface UserApiKeyService {

    /**
     * 创建用户 API Key
     *
     * @param request 创建请求
     * @return 创建响应（含明文 Key）
     */
    UserApiKeyCreateResponse create(UserApiKeyCreateRequest request);

    /**
     * 查询团队下的所有 API Key
     *
     * @param teamId 团队 ID
     * @return API Key 列表
     */
    List<UserApiKeyResponse> listByTeamId(Long teamId);

    /**
     * 查询单个 API Key（不含明文）
     *
     * @param id 主键
     * @return API Key 基本信息
     */
    UserApiKeyResponse getById(Long id);

    /**
     * 查询单个 API Key 详情（含明文 Key，用于页面复制）
     *
     * @param id 主键
     * @return API Key 详情（含明文）
     */
    UserApiKeyDetailResponse getDetailById(Long id);

    /**
     * 更新 API Key
     *
     * @param id      主键
     * @param request 更新请求
     * @return 更新后的 API Key
     */
    UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request);

    /**
     * 删除 API Key
     *
     * @param id 主键
     */
    void delete(Long id);
}