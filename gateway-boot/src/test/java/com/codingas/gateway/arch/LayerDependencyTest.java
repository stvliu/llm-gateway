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
package com.codingas.gateway.arch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 模块级依赖铁律（P4 硬规则，取代 P1 的 freeze 过渡态基线）。
 *
 * <p>验证 llm-gateway 模块化后的跨模块依赖方向。模块 = 根包（Jmix 式），核心模块
 * （纯领域逻辑 + 端口 API）与绑定模块（JPA DO/Repository/HTTP client）按根包判定。
 *
 * <p><b>模块根包约定</b>：
 * <ul>
 *   <li>核心模块根包：{@code provider / iam / usage / security / audit / alert / resilience / proxy / stats / protocol}</li>
 *   <li>绑定模块根包：{@code providerdata / iamdata / usagedata / securitydata / auditdata / alertdata / resiliencedata}</li>
 *   <li>{@code common} 保持纯横切：不依赖任何业务/绑定根包</li>
 *   <li>承载层：{@code boot}（启动装配）/ {@code adapter}（gateway-web Controller/Interceptor/Advice）</li>
 *   <li>starter 装配：{@code autoconfigure.<域>}（@AutoConfiguration + @Import 本域 Configuration）</li>
 * </ul>
 *
 * <p><b>P4 硬规则清单</b>：
 * <ol>
 *   <li>{@code NO_CORE_DEPENDS_BINDING_MODULES}：核心模块不得依赖绑定模块（P1 freeze → P4 硬规则）</li>
 *   <li>{@code NO_BINDING_CROSS_DOMAIN_DEPENDS}：绑定模块不得跨域依赖其他绑定模块（P1 freeze → P4 硬规则）</li>
 *   <li>{@code COMMON_NOT_DEPEND_ON_BUSINESS}：common 纯横切</li>
 *   <li>{@code NO_DEPENDS_ON_BOOT_OR_WEB}：业务域/绑定模块禁止反向依赖 boot/web 承载层</li>
 *   <li>{@code PROTOCOL_PLUGIN_ONLY_SPI}：协议插件只依赖协议核心 + 底座</li>
 *   <li>{@code PROTOCOL_PLUGIN_NO_COMPONENT}：协议插件包禁止普通 Spring 组件注解（防 boot 扫描穿透插件双注册）</li>
 *   <li>{@code STARTER_ONLY_AUTOCONFIGURE}：starter 装配类只依赖本域模块（禁止依赖其他业务域）</li>
 * </ol>
 *
 * <p>分析范围：仅主源码（{@link ImportOption.DoNotIncludeTests}）。测试类（尤其集成测试
 * 经 {@code @SpringBootTest(classes = GatewayApplication.class)} 全量起 boot）合法依赖承载层，
 * 不属于模块依赖铁律的约束对象。
 */
@AnalyzeClasses(packages = "com.codingas.gateway", importOptions = ImportOption.DoNotIncludeTests.class)
public class LayerDependencyTest {

    /** 核心模块根包（纯领域逻辑 + 端口 API，禁止依赖技术绑定） */
    private static final String[] CORE_MODULES = {
        "com.codingas.gateway.provider..",
        "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..",
        "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..",
        "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..",
        "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats..",
        "com.codingas.gateway.protocol.."
    };

    /** 绑定模块根包（JPA DO/Repository/HTTP client，核心模块物理不可见） */
    private static final String[] BINDING_MODULES = {
        "com.codingas.gateway.providerdata..",
        "com.codingas.gateway.iamdata..",
        "com.codingas.gateway.usagedata..",
        "com.codingas.gateway.securitydata..",
        "com.codingas.gateway.auditdata..",
        "com.codingas.gateway.alertdata..",
        "com.codingas.gateway.resiliencedata.."
    };

    /** 业务域 + 绑定模块根包（用于 NO_DEPENDS_ON_BOOT_OR_WEB 的源集合） */
    private static final String[] BUSINESS_AND_BINDING_MODULES = {
        "com.codingas.gateway.provider..",
        "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..",
        "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..",
        "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..",
        "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats..",
        "com.codingas.gateway.protocol..",
        "com.codingas.gateway.providerdata..",
        "com.codingas.gateway.iamdata..",
        "com.codingas.gateway.usagedata..",
        "com.codingas.gateway.securitydata..",
        "com.codingas.gateway.auditdata..",
        "com.codingas.gateway.alertdata..",
        "com.codingas.gateway.resiliencedata.."
    };

    /** 业务域根包（starter 装配禁止跨域依赖的源集合，不含绑定/承载层） */
    private static final String[] BUSINESS_MODULES = {
        "com.codingas.gateway.provider..",
        "com.codingas.gateway.iam..",
        "com.codingas.gateway.usage..",
        "com.codingas.gateway.security..",
        "com.codingas.gateway.audit..",
        "com.codingas.gateway.alert..",
        "com.codingas.gateway.resilience..",
        "com.codingas.gateway.proxy..",
        "com.codingas.gateway.stats.."
    };

    /** 绑定根包名（{@code com.codingas.gateway.<根包>} 的第一段） */
    private static final Set<String> BINDING_ROOTS = Set.of(
        "providerdata", "iamdata", "usagedata", "securitydata",
        "auditdata", "alertdata", "resiliencedata");

    /** 业务域根包名（含 protocol，用于跨域依赖判定） */
    private static final Set<String> BUSINESS_ROOTS = Set.of(
        "provider", "iam", "usage", "security", "audit", "alert",
        "resilience", "proxy", "stats", "protocol");

    /** 协议插件根包（openai/anthropic/gemini） */
    private static final String[] PROTOCOL_PLUGIN_PACKAGES = {
        "com.codingas.gateway.protocol.openai..",
        "com.codingas.gateway.protocol.anthropic..",
        "com.codingas.gateway.protocol.gemini.."
    };

    /**
     * 核心模块不得依赖绑定模块（P4 硬规则，原 P1 freeze 规则解冻）。
     * P1 过渡态已知违规（stats→providerdata/iamdata、auditdata 跨域 DO）已在 Task 2-4 清零。
     */
    @ArchTest
    static final ArchRule NO_CORE_DEPENDS_BINDING_MODULES = noClasses()
        .that().resideInAnyPackage(CORE_MODULES)
        .should().dependOnClassesThat().resideInAnyPackage(BINDING_MODULES);

    /**
     * 绑定模块不得依赖其他域绑定模块（跨域 DO/Repository 穿透，P4 硬规则，原 P1 freeze 规则解冻）。
     * P1 过渡态已知违规（auditdata→providerdata/iamdata DO）已在 Task 4 解耦为 ID 关联；
     * 同域绑定包内依赖（如 resiliencedata 内部）合法。
     *
     * <p>注意：自定义 {@link ArchCondition} 用 {@code classes()} 而非 {@code noClasses()}——
     * 条件内 {@code SimpleConditionEvent.violated} 事件在 {@code noClasses()} 语义下不判违规。
     */
    @ArchTest
    static final ArchRule NO_BINDING_CROSS_DOMAIN_DEPENDS = classes()
        .that().resideInAnyPackage(BINDING_MODULES)
        .should(onlyDependOnSameDomainBinding());

    /**
     * common 保持纯横切：不得依赖任何业务/绑定根包及遗留分层包（原「common 不依赖业务层」规则的模块化扩展）。
     */
    @ArchTest
    static final ArchRule COMMON_NOT_DEPEND_ON_BUSINESS = noClasses()
        .that().resideInAPackage("com.codingas.gateway.common..")
        .should().dependOnClassesThat().resideInAnyPackage(commonForbiddenTargets());

    /**
     * 业务域/绑定模块禁止反向依赖 boot/web 承载层。
     * boot（启动装配）与 adapter（gateway-web Controller/Interceptor/Advice）是承载层，
     * 业务域只向上暴露端口，不得反向依赖承载层实现。
     */
    @ArchTest
    static final ArchRule NO_DEPENDS_ON_BOOT_OR_WEB = noClasses()
        .that().resideInAnyPackage(BUSINESS_AND_BINDING_MODULES)
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.codingas.gateway.boot..", "com.codingas.gateway.adapter..");

    /**
     * 协议插件只依赖协议核心 + 底座（不依赖其他业务域）。
     * openai/anthropic/gemini 插件只可依赖 {@code protocol..} 与 {@code common..}，
     * 不得引用 provider/iam/usage/security/audit/alert/resilience/proxy/stats 等业务域。
     */
    @ArchTest
    static final ArchRule PROTOCOL_PLUGIN_ONLY_SPI = noClasses()
        .that().resideInAnyPackage(PROTOCOL_PLUGIN_PACKAGES)
        .should().dependOnClassesThat().resideInAnyPackage(BUSINESS_MODULES);

    /**
     * 协议插件包禁止 {@code @Component}/{@code @Service}/{@code @Repository}（防 boot 扫描穿透插件双注册）。
     *
     * <p>插件通过各自 {@code *AutoConfiguration}（@AutoConfiguration + @Bean）自包含装配，
     * 若插件类再挂普通组件注解会被 boot 组件扫描二次注册。@AutoConfiguration 本身允许
     * （其为 Spring Boot 装配入口，非普通组件）。实测确认 {@code @AutoConfiguration}
     * 类未被 {@code notBeAnnotatedWith(Component.class)} 误伤（元注解不判定为直接注解）。
     */
    @ArchTest
    static final ArchRule PROTOCOL_PLUGIN_NO_COMPONENT = classes()
        .that().resideInAnyPackage(PROTOCOL_PLUGIN_PACKAGES)
        .should().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
        .andShould().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
        .andShould().notBeAnnotatedWith(org.springframework.stereotype.Repository.class);

    /**
     * starter 装配类只依赖本域模块（禁止依赖其他业务域）。
     *
     * <p>每个 {@code autoconfigure.<域>} 包下的 {@code *AutoConfiguration} 只可依赖本域
     * 根包（如 {@code autoconfigure.provider} → {@code provider..} 的 Configuration），
     * 不得跨域引用其他业务域（provider/iam/usage/security/audit/alert/resilience/proxy/stats）。
     * 绑定层（*data）与承载层（boot/adapter）同样禁止。
     *
     * <p><b>取舍（两级 starter 模式）</b>：核心 starter 只依赖本域核心 Configuration，
     * 不直连 data 绑定模块——data 绑定装配由独立 {@code -data-starter} 承担（两级 starter：
     * 核心 starter + data-starter，对齐 Jmix {@code security-starter} + {@code security-data-starter}）。
     * 当前阶段 data 绑定模块由核心 Configuration 的 {@code @ComponentScan} 兼扫（2026-08-23 决策），
     * 故本规则禁止 starter 依赖 *data 绑定根包。
     *
     * <p>实现说明：不能直接用 {@code dependOnClassesThat().resideInAnyPackage(业务域)}——
     * 那样会连「本域模块」也判违规（如 ProviderAutoConfiguration → ProviderConfiguration
     * 属合法自引用）。故用自定义条件按「源类的域 == 目标类的域」判定，跨域即违规。
     */
    @ArchTest
    static final ArchRule STARTER_ONLY_AUTOCONFIGURE = classes()
        .that().resideInAnyPackage("com.codingas.gateway.autoconfigure..")
        .should(onlyDependOnOwnDomainOrNotBusiness());

    /**
     * 生成「只依赖同域绑定模块」条件：源绑定类对跨域绑定类（DO/Repository/HTTP client）的依赖判为违规。
     */
    private static ArchCondition<JavaClass> onlyDependOnSameDomainBinding() {
        return new ArchCondition<>("只依赖同域绑定模块（禁止跨域 DO/Repository 穿透）") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String originRoot = rootOf(item.getPackageName());
                item.getDirectDependenciesFromSelf().stream()
                    .map(dep -> dep.getTargetClass())
                    .filter(target -> isBindingRoot(target.getPackageName()))
                    .filter(target -> !rootOf(target.getPackageName()).equals(originRoot))
                    .forEach(target -> events.add(SimpleConditionEvent.violated(item,
                        String.format("%s 依赖跨域绑定模块 %s", item.getName(), target.getName()))));
            }
        };
    }

    /**
     * 生成「starter 装配只依赖本域模块」条件：源类所在的 {@code autoconfigure.<域>} 包，
     * 对目标类所在业务域根包（≠ 本域）的依赖判为违规；绑定层（*data）、承载层（boot/adapter）
     * 同样禁止。目标为 {@code common..} / 本域根包 / 外部框架类则合法。
     *
     * <p>绑定层（*data）禁止的取舍：核心 starter 不直连 data 绑定模块（两级 starter 模式，
     * data 装配归独立 {@code -data-starter}；当前阶段由核心 Configuration 兼扫，
     * 2026-08-23 决策），与 {@link #STARTER_ONLY_AUTOCONFIGURE} 同口径。
     */
    private static ArchCondition<JavaClass> onlyDependOnOwnDomainOrNotBusiness() {
        return new ArchCondition<>("只依赖本域模块（禁止跨域/绑定/承载层依赖）") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String autoconfigureDomain = domainOfAutoconfigure(item.getPackageName());
                item.getDirectDependenciesFromSelf().stream()
                    .map(dep -> dep.getTargetClass())
                    .forEach(target -> {
                        String targetRoot = rootOf(target.getPackageName());
                        // 仅业务域根包、绑定根包与承载层需要判定；common/框架类放行
                        if (BUSINESS_ROOTS.contains(targetRoot)) {
                            if (!targetRoot.equals(autoconfigureDomain)) {
                                events.add(SimpleConditionEvent.violated(item,
                                    String.format("%s 依赖其他业务域 %s", item.getName(), target.getName())));
                            }
                        } else if (BINDING_ROOTS.contains(targetRoot)
                                || "boot".equals(targetRoot) || "adapter".equals(targetRoot)) {
                            events.add(SimpleConditionEvent.violated(item,
                                String.format("%s 依赖非业务根包 %s", item.getName(), target.getName())));
                        }
                    });
            }
        };
    }

    /** common 规则禁止依赖的全部目标包（业务核心 + 绑定 + 承载层 + 遗留分层包） */
    private static String[] commonForbiddenTargets() {
        List<String> targets = new ArrayList<>(CORE_MODULES.length + BINDING_MODULES.length + 6);
        targets.addAll(Arrays.asList(CORE_MODULES));
        targets.addAll(Arrays.asList(BINDING_MODULES));
        targets.addAll(List.of(
            "com.codingas.gateway.boot..",
            "com.codingas.gateway.domain..",
            "com.codingas.gateway.application..",
            "com.codingas.gateway.infrastructure..",
            "com.codingas.gateway.adapter..",
            "com.codingas.gateway.api.."));
        return targets.toArray(String[]::new);
    }

    /** 判断目标包是否属于绑定根包 */
    private static boolean isBindingRoot(String packageName) {
        return BINDING_ROOTS.contains(rootOf(packageName));
    }

    /**
     * 提取 {@code autoconfigure.<域>} 的域根包名；非 autoconfigure 包返回空串。
     * 如 {@code com.codingas.gateway.autoconfigure.provider} → {@code provider}。
     */
    private static String domainOfAutoconfigure(String packageName) {
        String prefix = "com.codingas.gateway.autoconfigure.";
        if (!packageName.startsWith(prefix)) {
            return "";
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    /** 提取根包名：{@code com.codingas.gateway.<根包>.<子包>} → {@code <根包>}；非 gateway 包返回空串 */
    private static String rootOf(String packageName) {
        String prefix = "com.codingas.gateway.";
        if (!packageName.startsWith(prefix)) {
            return "";
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
