package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.domain.model.entity.ProviderCapabilities;
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
     * 测试连通性（支持分层结果）
     *
     * <p>执行分层连通性测试，返回详细的测试结果。</p>
     *
     * @param apiKey 待测试的 API Key
     * @param baseUrl 可选的 Base URL（为空使用默认值）
     * @param model 可选的测试模型（火山引擎等必填）
     * @return 分层测试结果
     */
    ConnectivityTestResult testConnectivity(String apiKey, String baseUrl, String model);

    /**
     * 获取此供应商的默认测试模型
     *
     * @return 默认测试模型名称，如果必须用户提供则返回 null
     */
    String getDefaultTestModel();

    /**
     * 是否需要用户提供测试模型
     *
     * <p>火山引擎等供应商需要用户提供 endpoint_id，无法使用默认模型。</p>
     *
     * @return true 如果必须用户提供模型参数
     */
    default boolean requiresUserProvidedModel() {
        return false;
    }

    /**
     * 获取此供应商的默认 Base URL
     *
     * @return 默认 Base URL
     */
    String getDefaultBaseUrl();

    /**
     * 获取 Provider 能力描述
     *
     * @return ProviderCapabilities
     */
    @Override
    ProviderCapabilities getCapabilities();
}
