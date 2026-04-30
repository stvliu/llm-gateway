package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitCreateRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitQueryRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitResponse;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitUpdateRequest;
import com.codingas.gateway.application.tokenlimit.TokenLimitService;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Token 限额管理控制器
 *
 * <p>提供 Token 限额 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/token-limits")
@RequiredArgsConstructor
public class TokenLimitController {

    private final TokenLimitService tokenLimitService;

    /**
     * 创建 Token 限额
     */
    @PostMapping
    public ApiResponse<TokenLimitResponse> create(@Valid @RequestBody TokenLimitCreateRequest request) {
        return ApiResponse.success(tokenLimitService.create(request));
    }

    /**
     * 获取 Token 限额详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TokenLimitResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(tokenLimitService.getById(id));
    }

    /**
     * 查询 Token 限额列表
     */
    @GetMapping
    public ApiResponse<PageResponse<TokenLimitResponse>> query(@ModelAttribute TokenLimitQueryRequest request) {
        return ApiResponse.success(tokenLimitService.query(request));
    }

    /**
     * 更新 Token 限额
     */
    @PutMapping("/{id}")
    public ApiResponse<TokenLimitResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TokenLimitUpdateRequest request) {
        return ApiResponse.success(tokenLimitService.update(id, request));
    }

    /**
     * 删除 Token 限额
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tokenLimitService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 重置已使用量
     */
    @PatchMapping("/{id}/reset-usage")
    public ApiResponse<TokenLimitResponse> resetUsage(@PathVariable Long id) {
        return ApiResponse.success(tokenLimitService.resetUsage(id));
    }
}
