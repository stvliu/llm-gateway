package com.codingas.gateway.infrastructure.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 模板同步配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "template.git")
public class TemplateSyncConfig {

    /**
     * Git 仓库地址
     */
    private String url = "https://github.com/codingas/llm-gateway-templates.git";

    /**
     * 分支名称
     */
    private String branch = "main";

    /**
     * 本地存储路径
     */
    private String localPath = System.getProperty("user.home") + "/.llm-gateway/templates";

    /**
     * 启动时是否同步
     */
    private boolean syncOnStartup = true;

    /**
     * 定时同步间隔（秒），0 表示禁用
     */
    private int syncInterval = 3600;
}