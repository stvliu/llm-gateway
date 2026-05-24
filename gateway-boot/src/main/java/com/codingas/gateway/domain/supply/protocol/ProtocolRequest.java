package com.codingas.gateway.domain.supply.protocol;

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
}