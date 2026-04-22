package com.codingas.gateway.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置属性
 *
 * <p>从 application.yml 读取网关相关配置。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayProperties {

    /** LLM 配置 */
    private LlmProperties llm = new LlmProperties();

    /** CORS 配置 */
    private CorsProperties cors = new CorsProperties();

    /** OpenTelemetry 配置 */
    private OtelProperties otel = new OtelProperties();

    /** 限流配置 */
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class LlmProperties {
        /** 路由策略 */
        private String routingStrategy = "PRIORITY";

        /** 最大重试次数 */
        private int maxRetries = 3;

        /** 超时时间 (秒) */
        private int timeoutSeconds = 30;

        /** 最大请求体大小 */
        private String maxRequestBodySize = "10MB";
    }

    @Getter
    @Setter
    public static class CorsProperties {
        /** 允许的来源 */
        private String allowedOrigins = "*";

        /** 允许的方法 */
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";

        /** 允许的头 */
        private String allowedHeaders = "*";

        /** 是否允许凭证 */
        private boolean allowCredentials = true;

        /** 预检请求缓存时间 */
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class OtelProperties {
        /** 是否启用 */
        private boolean enabled = false;

        /** OTLP 端点 */
        private String endpoint = "http://localhost:4317";

        /** 服务名称 */
        private String serviceName = "llm-gateway";
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        /** 是否启用 */
        private boolean enabled = true;

        /** 默认 RPM */
        private int defaultRpm = 1000;

        /** 默认 TPM */
        private int defaultTpm = 100000;
    }
}
