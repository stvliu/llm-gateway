package com.codingas.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 分级超时配置
 *
 * <p>支持全局默认超时和按通道/端点级别的超时覆盖。</p>
 *
 * <p>配置示例：</p>
 * <pre>
 * gateway:
 *   timeout:
 *     default-timeout-seconds: 60
 *     channel-timeouts:
 *       "100": 120
 *       "200": 30
 *     endpoint-timeouts:
 *       "300": 90
 * </pre>
 */
@ConfigurationProperties(prefix = "gateway.timeout")
public class GatewayTimeoutProperties {

    /** 全局默认超时（秒） */
    private int defaultTimeoutSeconds = 60;

    /** 按通道 ID 覆盖超时（秒） */
    private Map<String, Integer> channelTimeouts = new HashMap<>();

    /** 按端点 ID 覆盖超时（秒） */
    private Map<String, Integer> endpointTimeouts = new HashMap<>();

    public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
    public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) { this.defaultTimeoutSeconds = defaultTimeoutSeconds; }

    public Map<String, Integer> getChannelTimeouts() { return channelTimeouts; }
    public void setChannelTimeouts(Map<String, Integer> channelTimeouts) { this.channelTimeouts = channelTimeouts; }

    public Map<String, Integer> getEndpointTimeouts() { return endpointTimeouts; }
    public void setEndpointTimeouts(Map<String, Integer> endpointTimeouts) { this.endpointTimeouts = endpointTimeouts; }

    /**
     * 获取指定通道的超时时间
     *
     * @param channelId 通道 ID
     * @return 超时时间（秒），未配置时返回默认值
     */
    public int getTimeoutForChannel(Long channelId) {
        if (channelId == null) return defaultTimeoutSeconds;
        Integer timeout = channelTimeouts.get(String.valueOf(channelId));
        return timeout != null ? timeout : defaultTimeoutSeconds;
    }

    /**
     * 获取指定端点的超时时间
     *
     * @param endpointId 端点 ID
     * @return 超时时间（秒），未配置时返回默认值
     */
    public int getTimeoutForEndpoint(Long endpointId) {
        if (endpointId == null) return defaultTimeoutSeconds;
        Integer timeout = endpointTimeouts.get(String.valueOf(endpointId));
        return timeout != null ? timeout : defaultTimeoutSeconds;
    }
}