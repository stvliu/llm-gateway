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

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * COLA 分层架构依赖约束（模块化重构 ArchUnit 基座）。
 *
 * <p>验证 gateway-boot（及已拆出的 common/protocol/provider，其保持包名被
 * com.codingas.gateway.domain 等精确前缀覆盖）内的分层依赖方向。
 *
 * <p><b>精确包前缀</b>：使用 {@code com.codingas.gateway.application..} 而非 {@code ..application..}，
 * 避免误判 {@code domain.application} 领域子域（如 Application 依赖同包 ApplicationState）。
 *
 * <p><b>freeze 语义</b>：历史跨层违规被冻结为基线（待治理清单），重构逐项解冻修复。
 */
@AnalyzeClasses(packages = "com.codingas.gateway")
public class LayerDependencyTest {

    private static final String DOMAIN = "com.codingas.gateway.domain..";
    private static final String APPLICATION = "com.codingas.gateway.application..";
    private static final String INFRASTRUCTURE = "com.codingas.gateway.infrastructure..";
    private static final String ADAPTER = "com.codingas.gateway.adapter..";
    private static final String COMMON = "com.codingas.gateway.common..";

    /** domain 层不得依赖 infrastructure 层（依赖倒置：domain 只依赖 gateway 接口，实现走 infrastructure） */
    @ArchTest
    static final ArchRule DOMAIN_NOT_DEPEND_ON_INFRASTRUCTURE = freeze(
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE));

    /** domain 层不得依赖 application 层（应用层在上，领域层不反向依赖） */
    @ArchTest
    static final ArchRule DOMAIN_NOT_DEPEND_ON_APPLICATION = freeze(
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(APPLICATION));

    /** domain 层不得依赖 adapter 层（适配层在最上，领域层不反向依赖） */
    @ArchTest
    static final ArchRule DOMAIN_NOT_DEPEND_ON_ADAPTER = freeze(
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER));

    /** common 层保持纯横切，不得依赖任何业务层 */
    @ArchTest
    static final ArchRule COMMON_NOT_DEPEND_ON_BUSINESS = freeze(
        noClasses().that().resideInAPackage(COMMON)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, INFRASTRUCTURE, ADAPTER));
}
