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
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PriorityRouter 单元测试
 */
@DisplayName("PriorityRouter 单元测试")
class PriorityRouterTest {

    private final PriorityRouter router = new PriorityRouter();

    @Test
    @DisplayName("按应用级 priority 升序输出完整列表不收敛")
    void keepsAllInstances_sortedByPriorityAscending() {
        // 渠道 10/20/30 对应应用级 priority 100/200/100；按映射升序：10(100)→30(100)→20(200) → [mi1, mi3, mi2]
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setChannelId(30L);

        Map<Long, Integer> channelPriorityMap = Map.of(10L, 100, 20L, 200, 30L, 100);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2, mi3), request);

        // 不收敛：保留全部 3 个；按应用级 priority 升序：100,100,200 → [mi1, mi3, mi2]
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 3L, 2L);
    }

    @Test
    @DisplayName("主备 priority 不同时输出完整列表 [主,备] 不丢备")
    void primaryAndBackup_differentPriority_keepsFullList() {
        // 渠道 10(主 priority=1)、渠道 20(备 priority=2)
        ModelInstance primary = new ModelInstance();
        primary.setId(1L);
        primary.setChannelId(10L);
        ModelInstance backup = new ModelInstance();
        backup.setId(2L);
        backup.setChannelId(20L);

        Map<Long, Integer> channelPriorityMap = Map.of(10L, 1, 20L, 2);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);
        List<ModelInstance> result = router.filter(List.of(primary, backup), request);

        // 主备不丢：返回 [主,备]，应用级 priority 升序（L1 故障转移前提）
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("单 priority 组返回全部")
    void singlePriority_returnsAll() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);

        Map<Long, Integer> channelPriorityMap = Map.of(10L, 100, 20L, 100);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("应用级映射为空时所有实例回退默认值 100 并保持原序")
    void nullPriority_usesDefault() {
        // 应用级映射为空（如 applicationId 为 null）：全部回退 100，稳定排序保持原序
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);

        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, Map.of());
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        // 映射为空，全部回退 100，稳定排序保持原序 → [mi1, mi2]
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("空列表返回空")
    void emptyInput_returnsEmpty() {
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, Map.of());
        List<ModelInstance> result = router.filter(List.of(), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }

    @Test
    @DisplayName("按应用级 channelPriorityMap 升序排序，覆盖实例 priority")
    void sortsByApplicationChannelPriorityMap() {
        // 三个实例分属渠道 10/20/30；应用级映射为 10->200, 20->1, 30->100
        // 实例自身 priority（100/200/300）应被应用级映射覆盖，按映射升序：20(1)→30(100)→10(200)
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        mi1.setPriority(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);
        mi2.setPriority(200);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setChannelId(30L);
        mi3.setPriority(300);

        Map<Long, Integer> channelPriorityMap = Map.of(10L, 200, 20L, 1, 30L, 100);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);

        List<ModelInstance> result = router.filter(List.of(mi1, mi2, mi3), request);

        // 应用级映射精排：渠道 20(1) → 渠道 30(100) → 渠道 10(200)
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("channelPriorityMap 为空时所有实例回退默认值 100，保持原顺序")
    void emptyPriorityMap_fallsBackToDefault() {
        // 空映射：所有渠道回退 100，稳定排序保持输入顺序
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        mi1.setPriority(1);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);
        mi2.setPriority(2);

        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, Map.of());

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
        // 映射为空，全部回退 100，稳定排序保持原序 [mi1, mi2]
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("channelPriorityMap 中不存在的渠道回退默认值 100")
    void channelNotInMap_fallsBackToDefault() {
        // 渠道 10 在映射中 priority=200；渠道 20 不在映射中，回退 100
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(10L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);

        Map<Long, Integer> channelPriorityMap = Map.of(10L, 200);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        // 渠道 20 回退 100 优先于 渠道 10 的 200 → [mi2, mi1]
        assertThat(result).extracting(ModelInstance::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("同渠道不同应用不同 priority 各自转移顺序独立")
    void sameChannelDifferentApp_independentOrder() {
        // 同一组实例（渠道 10/20），应用 A 映射 10->1, 20->2；应用 B 映射 10->2, 20->1
        ModelInstance miA1 = new ModelInstance();
        miA1.setId(1L);
        miA1.setChannelId(10L);
        ModelInstance miA2 = new ModelInstance();
        miA2.setId(2L);
        miA2.setChannelId(20L);

        // 应用 A：渠道 10 优先（priority 1 < 2）→ [miA1, miA2]
        Map<Long, Integer> appAMap = Map.of(10L, 1, 20L, 2);
        RoutingRequest requestA = new RoutingRequest(
                1L, 100L, 1L, "USER", RoutingStrategy.WEIGHTED, null, appAMap);
        List<ModelInstance> resultA = router.filter(List.of(miA1, miA2), requestA);
        assertThat(resultA).extracting(ModelInstance::getId).containsExactly(1L, 2L);

        // 应用 B：渠道 20 优先（priority 1 < 2）→ [miA2, miA1]
        Map<Long, Integer> appBMap = Map.of(10L, 2, 20L, 1);
        RoutingRequest requestB = new RoutingRequest(
                1L, 200L, 1L, "USER", RoutingStrategy.WEIGHTED, null, appBMap);
        List<ModelInstance> resultB = router.filter(List.of(miA1, miA2), requestB);
        assertThat(resultB).extracting(ModelInstance::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("实例 channelId 为 null 时回退默认值不抛 NPE")
    void nullChannelId_fallsBackToDefaultWithoutNpe() {
        // 实例未设置 channelId（如旧测试数据）；不可变 Map 不允许 null key，须防护
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(null);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(20L);

        Map<Long, Integer> channelPriorityMap = Map.of(20L, 200);
        RoutingRequest request = new RoutingRequest(
                1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null, channelPriorityMap);

        // mi1 channelId=null 回退 100，mi2=200；升序 [mi1(100), mi2(200)]，不抛 NPE
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
    }
}
