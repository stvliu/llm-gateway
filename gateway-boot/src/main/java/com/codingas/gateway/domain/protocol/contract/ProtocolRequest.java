/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.protocol.contract;

/**
 * 协议请求接口，所有协议请求 DTO 实现此接口
 */
public interface ProtocolRequest {

    /**
     * 获取模型名称
     */
    String getModel();

    /**
     * 设置模型名称（路由后覆盖）
     */
    void setModel(String model);

    /**
     * 获取协议标识（"openai" / "anthropic"）
     */
    String getProtocol();

    /**
     * 是否流式请求
     */
    boolean isStream();

    /**
     * 设置流式标记
     */
    void setStream(boolean stream);

    /**
     * 创建当前请求的同类型副本（手写字段拷贝）
     *
     * <p>用于调谐下沉场景：{@code ChannelFailoverInvoker} 对每个候选渠道基于原始请求的副本
     * 独立执行 convert+tune，避免候选间相互污染（如模型名替换覆盖原始请求）。
     * 不使用 Jackson 深拷贝，避免性能开销与循环引用风险。</p>
     *
     * <p>默认实现抛 {@link UnsupportedOperationException}，强制具体协议请求 DTO
     * （{@link OpenAIChatRequest}/{@link AnthropicMessagesRequest}）覆盖。测试用匿名实现
     * 不参与调谐下沉（仅直连 KeyFailoverInvoker/UpstreamClient），不会被调用。</p>
     *
     * @return 与当前请求同类型的独立副本（业务字段逐一拷贝）
     */
    default ProtocolRequest copy() {
        throw new UnsupportedOperationException(
                "copy() 必须由具体协议请求 DTO 实现（OpenAIChatRequest/AnthropicMessagesRequest）");
    }
}