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
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.provider.model.ModelInstance;
import java.util.List;

/**
 * 路由器接口 — 对候选实例列表执行过滤，返回符合条件的子集
 */
@FunctionalInterface
public interface Router {

    List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request);

    default boolean isForce() { return false; }
}
