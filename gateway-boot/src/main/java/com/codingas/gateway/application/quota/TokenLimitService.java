/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.quota;

import com.codingas.gateway.application.quota.dto.TokenLimitCreateRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitQueryRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitResponse;
import com.codingas.gateway.application.quota.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * Token 限额应用服务接口
 *
 * <p>处理 Token 限额管理的业务逻辑。</p>
 */
public interface TokenLimitService {

    /**
     * 创建 Token 限额
     */
    TokenLimitResponse create(TokenLimitCreateRequest request);

    /**
     * 根据 ID 获取 Token 限额
     */
    TokenLimitResponse getById(Long id);

    /**
     * 查询 Token 限额列表
     */
    PageResponse<TokenLimitResponse> query(TokenLimitQueryRequest request);

    /**
     * 更新 Token 限额
     */
    TokenLimitResponse update(Long id, TokenLimitUpdateRequest request);

    /**
     * 删除 Token 限额（软删除）
     */
    void delete(Long id);

    /**
     * 重置已使用量
     */
    TokenLimitResponse resetUsage(Long id);
}
