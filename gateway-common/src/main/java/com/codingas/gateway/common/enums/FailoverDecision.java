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
package com.codingas.gateway.domain.supply.enums;

/**
 * 故障转移决策枚举
 *
 * <p>由 {@link com.codingas.gateway.application.proxy.failover.ErrorClassifier} 依据
 * {@link ProviderErrorType} 产出，指导故障转移策略层级选择。</p>
 *
 * <ul>
 *   <li>{@link #L1} — 换渠道共因故障转移：同一 Provider 内换 Key/Endpoint，解决认证/限流/配额/网络等共因故障</li>
 *   <li>{@link #NONE} — 不转移：直接抛出原异常，适用于请求级错误（换哪都无效）或无法判定的错误</li>
 * </ul>
 *
 * <p><b>Task 4 变更</b>：L2 模型降级层已删除，{@code L2} 枚举值移除。模型降级决策交还给应用层，
 * 容灾栈从四层（L0/L1/L2/L3）收敛为三层（L0/L1/L3）。</p>
 */
public enum FailoverDecision {
    /** L1：换渠道共因故障转移（认证/限流/配额/网络/超时/上游错误等共因故障） */
    L1,

    /** NONE：不转移直接抛出原异常（请求级错误或无法判定错误） */
    NONE
}
