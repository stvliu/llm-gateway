/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.iam.valueobject.Identity;

/**
 * 认证应用服务接口
 *
 * <p>编排认证相关的领域服务，不含业务逻辑。</p>
 */
public interface AuthService {

    /**
     * 认证 API Key
     *
     * @param apiKey API Key
     * @param clientIp 客户端 IP
     * @return 认证结果
     */
    Identity authenticate(String apiKey, String clientIp);
}