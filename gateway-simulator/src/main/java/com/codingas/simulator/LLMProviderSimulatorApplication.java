package com.codingas.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LLM 提供商模拟器启动类。
 * <p>
 * 模拟 OpenAI 和 Anthropic 的 API 端点，用于本地开发和集成测试。
 */
@SpringBootApplication
public class LLMProviderSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LLMProviderSimulatorApplication.class, args);
    }
}
