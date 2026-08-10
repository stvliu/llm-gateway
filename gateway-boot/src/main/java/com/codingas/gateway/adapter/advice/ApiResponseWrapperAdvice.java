/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应包装处理器
 *
 * <p>自动将 Controller 返回的业务对象包装为 ApiResponse 格式。</p>
 * <p>Controller 只需返回业务对象，无需手动调用 ApiResponse.success()。</p>
 */
@Slf4j
@RestControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否需要包装响应
     *
     * <p>排除以下情况：</p>
     * <ul>
     *   <li>已经是 ApiResponse 类型</li>
     *   <li>已经是 ResponseEntity&lt;ApiResponse&gt; 类型（异常处理返回）</li>
     *   <li>Actuator 端点</li>
     *   <li>OpenAPI 文档端点</li>
     * </ul>
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 已经是 ApiResponse 类型，不需要包装
        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }

        // ResponseEntity<ApiResponse> 类型，不需要包装（异常处理器返回）
        if (returnType.getParameterType().equals(org.springframework.http.ResponseEntity.class)) {
            return false;
        }

        // 获取方法所在的类
        Class<?> declaringClass = returnType.getDeclaringClass();
        String className = declaringClass.getName();

        // 排除 Spring 内部端点
        if (className.startsWith("org.springframework.")) {
            return false;
        }

        return true;
    }

    /**
     * 在响应写入前包装业务对象
     */
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        String path = request.getURI().getPath();

        // 排除 Actuator、OpenAPI、LLM 代理端点
        // LLM 代理端点（/v1/chat/completions、/anthropic/v1/messages）需要返回原始格式
        if (path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger")
                || path.startsWith("/v1/chat")
                || path.startsWith("/anthropic/")) {
            return body;
        }

        // 已经是 ApiResponse 类型，直接返回（避免双重包装）
        if (body instanceof ApiResponse) {
            return body;
        }

        // void 返回类型，返回成功响应无数据
        if (body == null) {
            return ApiResponse.success();
        }

        // String 类型需要特殊处理，因为 StringHttpMessageConverter 会直接写入
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return JsonUtils.toJson(ApiResponse.success(body));
        }

        // 其他类型正常包装
        return ApiResponse.success(body);
    }
}
