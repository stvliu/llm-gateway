package com.codingas.gateway.domain.proxy.valueobject;

/**
 * 连通性测试结果值对象（Domain 层）
 *
 * <p>用于协议网关的连通性测试返回。</p>
 */
public record ConnectivityTestResultVO(
        boolean success,
        String message,
        String model,
        Integer latencyMs,
        ErrorVO error
) {

    /**
     * 错误值对象
     */
    public record ErrorVO(
            String type,
            String code,
            String message
    ) {}

    /**
     * 创建成功结果
     */
    public static ConnectivityTestResultVO success(String model, Integer latencyMs) {
        return new ConnectivityTestResultVO(true, "连通性测试成功", model, latencyMs, null);
    }

    /**
     * 创建失败结果
     */
    public static ConnectivityTestResultVO failure(String message, String errorType, String errorCode) {
        return new ConnectivityTestResultVO(false, message, null, null,
                new ErrorVO(errorType, errorCode, message));
    }
}