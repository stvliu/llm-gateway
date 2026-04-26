package com.codingas.gateway.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关配置属性
 *
 * <p>从 application.yml 读取网关相关配置。</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayProperties {

    private LlmProperties llm = new LlmProperties();
    private CorsProperties cors = new CorsProperties();
    private OtelProperties otel = new OtelProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class LlmProperties {
        private String routingStrategy = "PRIORITY";
        private int maxRetries = 3;
        private int timeoutSeconds = 30;
        private String maxRequestBodySize = "10MB";
    }

    @Getter
    @Setter
    public static class CorsProperties {
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class OtelProperties {
        private boolean enabled = false;
        private String endpoint = "http://localhost:4317";
        private String serviceName = "llm-gateway";
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        private boolean enabled = true;
        private int defaultRpm = 1000;
        private int defaultTpm = 100000;
    }
}
