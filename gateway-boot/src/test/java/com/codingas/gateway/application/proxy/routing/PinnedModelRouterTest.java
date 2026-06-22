package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PinnedModelRouter 单元测试
 *
 * <p>验证画像锁定语义：{@code profile.enablePinnedModel} 开启时，只保留 {@code pinnedModelId}
 * 对应的实例（按 modelId 匹配）；关闭或不设 pinnedModelId 时透传全部候选。
 * Router 排在 PriorityRouter(300) 之后（@Order 350）。</p>
 */
@DisplayName("PinnedModelRouter 单元测试")
class PinnedModelRouterTest {

    private final PinnedModelRouter router = new PinnedModelRouter();

    @Test
    @DisplayName("enablePinnedModel 开启：只保留 pinnedModelId 对应实例")
    void pinnedEnabled_keepsOnlyPinnedModel() {
        ModelInstance pinned = instance(1L, 100L);
        ModelInstance other = instance(2L, 200L);
        ResilienceProfile profile = profile(true, 100L);

        RoutingRequest request = request(profile);
        List<ModelInstance> result = router.filter(List.of(pinned, other), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("enablePinnedModel 关闭：透传全部候选")
    void pinnedDisabled_passesAll() {
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 200L);
        ResilienceProfile profile = profile(false, 100L);

        RoutingRequest request = request(profile);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("enablePinnedModel 开启但 pinnedModelId 为 null：透传全部（无锁定目标）")
    void pinnedEnabledButNullId_passesAll() {
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 200L);
        ResilienceProfile profile = profile(true, null);

        RoutingRequest request = request(profile);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("profile 为 null：透传全部（无画像不锁定）")
    void nullProfile_passesAll() {
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 200L);

        RoutingRequest request = request(null);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("锁定模型不在候选中：返回空（候选无 pinnedModelId 实例）")
    void pinnedNotInCandidates_returnsEmpty() {
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 200L);
        // 锁定 modelId=300，候选均不匹配
        ResilienceProfile profile = profile(true, 300L);

        RoutingRequest request = request(profile);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isForce 返回 false（锁定后空则让链继续）")
    void isForce_returnsFalse() {
        assertThat(router.isForce()).isFalse();
    }

    /** 构造测试用 ModelInstance */
    private ModelInstance instance(long id, long modelId) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setModelId(modelId);
        return mi;
    }

    /** 构造测试用 ResilienceProfile */
    private ResilienceProfile profile(boolean enablePinned, Long pinnedModelId) {
        ResilienceProfile p = new ResilienceProfile();
        p.setEnablePinnedModel(enablePinned);
        p.setPinnedModelId(pinnedModelId);
        return p;
    }

    /** 构造携带画像的路由请求 */
    private RoutingRequest request(ResilienceProfile profile) {
        return new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI, profile);
    }
}
