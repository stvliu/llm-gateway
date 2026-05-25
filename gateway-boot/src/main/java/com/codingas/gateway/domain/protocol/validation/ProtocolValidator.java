package com.codingas.gateway.domain.protocol.validation;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;

/**
 * 协议校验器接口
 *
 * @param <T> 协议请求类型
 */
public interface ProtocolValidator<T extends ProtocolRequest> {

    /**
     * 获取支持的协议标识
     */
    String getProtocol();

    /**
     * 入站校验协议请求
     */
    void validate(T request);
}
