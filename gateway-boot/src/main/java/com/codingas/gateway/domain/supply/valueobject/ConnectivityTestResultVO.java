package com.codingas.gateway.domain.supply.valueobject;

/**
 * 连通性测试结果值对象
 *
 * <p>用于协议网关的连通性测试返回。</p>
 */
public record ConnectivityTestResultVO(
        boolean success,
        Long channelId,
        String errorMessage,
        long latencyMs
) {

    /**
     * 创建成功结果
     */
    public static ConnectivityTestResultVO success(Long channelId, long latencyMs) {
        return new ConnectivityTestResultVO(true, channelId, null, latencyMs);
    }

    /**
     * 创建失败结果
     */
    public static ConnectivityTestResultVO failure(Long channelId, String errorMessage) {
        return new ConnectivityTestResultVO(false, channelId, errorMessage, 0);
    }
}