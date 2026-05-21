package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户信息控制器
 *
 * <p>提供 /api/v1/me/* 路径下的端点，操作当前登录用户的资源。</p>
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserApiKeyService userApiKeyService;

    /**
     * 查询当前用户的所有 API Key
     */
    @GetMapping("/api-keys")
    public List<UserApiKeyResponse> listMyApiKeys() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userApiKeyService.findByUserId(userId);
    }
}
