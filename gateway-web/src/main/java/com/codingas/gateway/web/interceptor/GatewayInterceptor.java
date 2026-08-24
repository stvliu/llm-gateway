/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 网关拦截器接口
 *
 * <p>所有安全拦截器实现此接口，支持责任链模式。</p>
 */
public interface GatewayInterceptor {

    /**
     * 拦截器名称
     */
    String name();

    /**
     * 处理请求
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @return true 继续执行后续拦截器，false 短路处理
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取执行顺序（数字越小越靠前）
     */
    int order();
}
