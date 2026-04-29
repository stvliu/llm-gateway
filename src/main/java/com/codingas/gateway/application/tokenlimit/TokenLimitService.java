package com.codingas.gateway.application.tokenlimit;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitCreateRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitQueryRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitResponse;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitUpdateRequest;
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