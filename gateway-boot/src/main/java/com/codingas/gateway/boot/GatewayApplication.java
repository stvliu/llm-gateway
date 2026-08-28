/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * <p>scanBasePackages 限定 boot 自身包（config/init/actuator/event 收拢于此）+ 底座
 * （common/protocol）——CommandLineRunner 初始化种子与配置组件由本扫描装配；
 * 业务域组件由各域 starter 的 AutoConfiguration 显式装配（装配显式化，去全包扫描）；
 * HTTP 承载层（Controller/Interceptor/Advice）由 gateway-web 的
 * {@code WebAutoConfiguration} 装配（boot 依赖 gateway-web 即生效）。</p>
 *
 * <p>JPA 说明：启动类移入 {@code boot} 包后，Spring Data 的自动配置包（{@code AutoConfigurationPackage}）
 * 随之收窄到 {@code com.codingas.gateway.boot}，业务域绑定模块（-data）的 JPA 实体与
 * Repository 不再被隐式扫描，故显式声明 {@link EntityScan} 与 {@link EnableJpaRepositories}
 * 覆盖各域 data 根包（装配显式化的配套收窄）。</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.codingas.gateway.boot",
        "com.codingas.gateway.common",
        "com.codingas.gateway.protocol"
})
@EnableJpaRepositories(basePackages = {
        "com.codingas.gateway.providerdata",
        "com.codingas.gateway.iamdata",
        "com.codingas.gateway.usagedata",
        "com.codingas.gateway.securitydata",
        "com.codingas.gateway.resiliencedata",
        "com.codingas.gateway.auditdata",
        "com.codingas.gateway.settingsdata"
})
@EntityScan(basePackages = {
        "com.codingas.gateway.providerdata",
        "com.codingas.gateway.iamdata",
        "com.codingas.gateway.usagedata",
        "com.codingas.gateway.securitydata",
        "com.codingas.gateway.resiliencedata",
        "com.codingas.gateway.auditdata",
        "com.codingas.gateway.alertdata",
        "com.codingas.gateway.settingsdata"
})
@EnableScheduling
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
