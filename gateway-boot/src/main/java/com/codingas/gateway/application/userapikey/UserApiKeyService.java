package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;

import java.util.List;

/**
 * 用户 API Key 应用服务接口
 */
public interface UserApiKeyService {

    UserApiKeyCreateResponse create(UserApiKeyCreateRequest request);

    List<UserApiKeyResponse> listByTeamId(Long teamId);

    /** 按用户 ID 查找 API Key 列表 */
    List<UserApiKeyResponse> findByUserId(Long userId);

    UserApiKeyResponse getById(Long id);

    UserApiKeyDetailResponse getDetailById(Long id);

    /**
     * 获取 API Key 详情并校验团队归属
     *
     * @param id     API Key ID
     * @param teamId 期望的团队 ID，用于权限校验
     * @return API Key 详情（含明文）
     * @throws IllegalArgumentException 如果 API Key 不存在或不属于该团队
     */
    UserApiKeyDetailResponse getDetailByIdAndTeamId(Long id, Long teamId);

    UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request);

    void delete(Long id);
}