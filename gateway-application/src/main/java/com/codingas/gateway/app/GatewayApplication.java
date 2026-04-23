package com.codingas.gateway.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LLM-Gateway 应用主入口
 *
 * <p>企业级 AI 模型 API 聚合分发与智能路由网关，支持 OpenAI 和 Anthropic 双 API 标准。</p>
 *
 * @see <a href="https://github.com/llm-gateway/llm-gateway">LLM-Gateway</a>
 */
@SpringBootApplication(scanBasePackages = "com.llm.gateway")
@ConfigurationPropertiesScan(basePackages = "com.llm.gateway")
@EnableAsync
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
