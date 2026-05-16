package com.codingas.gateway.infrastructure.metadata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * 元数据同步配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "metadata")
public class MetadataSyncConfig {

    /**
     * 内置元数据同步配置
     */
    private Builtin builtin = new Builtin();

    /**
     * Models.dev 同步配置
     */
    private ModelsDev modelsDev = new ModelsDev();

    @Data
    public static class Builtin {
        /**
         * 启动时是否同步内置元数据
         */
        private boolean syncOnStartup = true;
    }

    @Data
    public static class ModelsDev {
        /**
         * Models.dev API 地址
         */
        private String apiUrl = "https://models.dev/api.json";

        /**
         * 是否启用 Models.dev 同步
         */
        private boolean enabled = false;

        /**
         * 启动时是否同步
         */
        private boolean syncOnStartup = false;

        /**
         * 定时同步间隔（毫秒），默认24小时
         */
        private long syncInterval = 86400000;

        /**
         * 启动后首次同步延迟（毫秒），默认5分钟
         */
        private long initialDelay = 300000;

        /**
         * 连接超时（秒），默认10秒
         */
        private int connectTimeoutSeconds = 10;

        /**
         * 读取超时（秒），默认30秒
         */
        private int readTimeoutSeconds = 30;

        /**
         * 支持同步的供应商ID列表
         */
        private Set<String> supportedProviders = Set.of(
            "openai", "anthropic", "google", "deepseek", "moonshot",
            "zhipu", "baichuan", "minimax", "volcengine",
            "qwen", "wenxin", "tencent", "xunfei"
        );
    }
}
