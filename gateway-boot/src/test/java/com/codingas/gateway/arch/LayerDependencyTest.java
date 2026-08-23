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
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * 模块级依赖约束（P1 模块化重构 ArchUnit 基座，取代过时的 COLA 分层规则）。
 *
 * <p>验证 llm-gateway 模块化后的跨模块依赖方向。模块 = 根包（Jmix 式），核心模块
 * （纯领域逻辑 + 端口 API）与绑定模块（JPA DO/Repository/HTTP client）按根包判定。
 *
 * <p><b>模块根包约定</b>：
 * <ul>
 *   <li>核心模块根包：{@code provider / iam / usage / security / audit / alert / resilience / proxy / stats / protocol}</li>
 *   <li>绑定模块根包：{@code providerdata / iamdata / usagedata / securitydata / auditdata / alertdata / resiliencedata / providerhttp}</li>
 *   <li>{@code common} 保持纯横切：不依赖任何业务/绑定根包</li>
 * </ul>
 *
 * <p><b>freeze 语义</b>：P1 过渡态的已知跨模块 infrastructure 依赖被冻结为违规基线
 * （记录于 {@code target/archunit/}），P4 解耦为端口调用/ID 关联后逐项解冻。
 * 当前冻结基线（验证时实测）：
 * <ul>
 *   <li>stats→providerdata/iamdata（StatsService 引 Repository）</li>
 *   <li>resilience→providerhttp（ResilientClientFactoryImpl 引 upstream client）</li>
 *   <li>proxy→providerhttp（ChatDispatchServiceImpl 经 provider-http 传递依赖引 SseErrorFormatter）</li>
 *   <li>auditdata→providerdata/iamdata（UsageLogDo 引跨域 DO）</li>
 * </ul>
 * alert→iamdata 的 DO 依赖（AlertNotificationDo）因 gateway-alert-data 尚未进入 gateway-boot 依赖，
 * 暂不在分析范围内，P4 补齐依赖后由 {@code NO_BINDING_CROSS_DOMAIN_DEPENDS} 规则接住。
 */
@AnalyzeClasses(packages = "com.codingas.gateway")
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
        "com.codingas.gateway.resiliencedata..",
        "com.codingas.gateway.providerhttp.."
    };

    /** 绑定根包名（{@code com.codingas.gateway.<根包>} 的第一段） */
    private static final Set<String> BINDING_ROOTS = Set.of(
        "providerdata", "iamdata", "usagedata", "securitydata",
        "auditdata", "alertdata", "resiliencedata", "providerhttp");

    /**
     * 核心模块不得依赖绑定模块（模块依赖 API 铁律雏形）。
     * P1 过渡态已知违规（stats→providerdata/iamdata、resilience→providerhttp）冻结为基线，P4 解冻。
     */
    @ArchTest
    static final ArchRule NO_CORE_DEPENDS_BINDING_MODULES = freeze(
        noClasses()
            .that().resideInAnyPackage(CORE_MODULES)
            .should().dependOnClassesThat().resideInAnyPackage(BINDING_MODULES));

    /**
     * 绑定模块不得依赖其他域绑定模块（跨域 DO/Repository 穿透）。
     * P1 过渡态已知违规（auditdata→providerdata/iamdata DO）冻结为基线，
     * P4 解耦为 ID 关联/端口调用；同域绑定包内依赖（如 resiliencedata 内部）合法。
     *
     * <p>注意：自定义 {@link ArchCondition} 用 {@code classes()} 而非 {@code noClasses()}——
     * 条件内 {@code SimpleConditionEvent.violated} 事件在 {@code noClasses()} 语义下不判违规。
     */
    @ArchTest
    static final ArchRule NO_BINDING_CROSS_DOMAIN_DEPENDS = freeze(
        classes()
            .that().resideInAnyPackage(BINDING_MODULES)
            .should(onlyDependOnSameDomainBinding()));

    /**
     * common 保持纯横切：不得依赖任何业务/绑定根包及遗留分层包（原「common 不依赖业务层」规则的模块化扩展）。
     */
    @ArchTest
    static final ArchRule COMMON_NOT_DEPEND_ON_BUSINESS = noClasses()
        .that().resideInAPackage("com.codingas.gateway.common..")
        .should().dependOnClassesThat().resideInAnyPackage(commonForbiddenTargets());

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

    /** common 规则禁止依赖的全部目标包（业务核心 + 绑定 + 遗留分层包） */
    private static String[] commonForbiddenTargets() {
        List<String> targets = new ArrayList<>(CORE_MODULES.length + BINDING_MODULES.length + 5);
        targets.addAll(Arrays.asList(CORE_MODULES));
        targets.addAll(Arrays.asList(BINDING_MODULES));
        targets.addAll(List.of(
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
