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
package com.codingas.gateway.provider.catalog;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量开通请求
 *
 * <p>供应商级联开通时，可选择性指定套餐编码列表。不传则开通所有 ACTIVE 套餐。</p>
 */
@Getter
@Setter
public class BatchProvisionRequest {

    /** 需要开通的套餐编码列表（可选，不传则全部开通） */
    private List<String> planCodes;
}