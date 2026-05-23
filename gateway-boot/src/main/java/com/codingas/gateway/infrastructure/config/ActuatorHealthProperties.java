package com.codingas.gateway.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Actuator 安全配置属性
 *
 * <p>控制 /actuator/health 端点是否跳过认证拦截。</p>
 */
@Data
@ConfigurationProperties(prefix = "gateway.actuator.health")
public class ActuatorHealthProperties {

    /** health 端点是否公开访问（无需认证） */
    private boolean publicAccess = true;
}