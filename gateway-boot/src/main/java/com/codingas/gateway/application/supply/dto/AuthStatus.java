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
package com.codingas.gateway.application.supply.dto;

/**
 * 单 Key 认证状态
 *
 * <ul>
 *   <li>PASS：连通性测试通过</li>
 *   <li>FAIL：测试失败（认证错误、网络错误等）</li>
 *   <li>TIMEOUT：测试超时（单 Key 5s 超时）</li>
 * </ul>
 */
public enum AuthStatus {
    PASS,
    FAIL,
    TIMEOUT
}
