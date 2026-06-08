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

    List<UserApiKeyResponse> findByUserId(Long userId);

    /** 查询所有非删除状态的 Key（管理员用） */
    List<UserApiKeyResponse> findAllNonDeleted();

    UserApiKeyResponse getById(Long id);

    UserApiKeyDetailResponse getDetailById(Long id);

    UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request);

    void delete(Long id);
}