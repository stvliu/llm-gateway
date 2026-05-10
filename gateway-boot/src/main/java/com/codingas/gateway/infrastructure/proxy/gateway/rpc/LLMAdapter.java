package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;

/**
 * LLM 适配器接口
 *
 * <p>所有 LLM 提供商 (OpenAI, Anthropic, 等) 必须实现此接口。</p>
 * <p>实现 Domain 层定义的 LLMGateway 接口。</p>
 */
public interface LLMAdapter extends LLMGateway {

    /**
     * 获取提供商编码
     *
     * @return 提供商唯一编码 (如 "openai", "anthropic")
     */
    @Override
    String getProviderCode();

    /**
     * 获取提供商类型
     *
     * @return 提供商类型枚举
     */
    @Override
    ProviderType getProviderType();

    /**
     * 检查适配器是否可用
     *
     * @return true 如果适配器已配置并可用
     */
    @Override
    boolean isAvailable();

    /**
     * 健康检查
     *
     * @return true 表示 Provider 可用
     */
    @Override
    boolean isHealthy();

    /**
     * 执行连接检查
     *
     * <p>实际验证与提供商的连接是否正常，用于故障转移判断。</p>
     *
     * @return true 如果连接正常
     */
    boolean checkConnection();

    /**
     * 获取 Provider 能力描述
     *
     * @return ProviderCapabilities
     */
    @Override
    ProviderCapabilities getCapabilities();
}
