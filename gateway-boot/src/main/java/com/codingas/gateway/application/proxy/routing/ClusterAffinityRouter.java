package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster 故障域亲和路由器 — 按域锁定，过滤整域故障（DOWN）的实例
 *
 * <p>顺序语义：排在 {@link HealthRouter}（@Order 200）之后、{@link PriorityRouter}（@Order 300）
 * 之前（@Order 250）。先由健康路由剔除单端点熔断的实例，再由本路由器在存活候选中按域聚合判断——
 * 域内全部端点熔断（Cluster DOWN）则过滤整域实例，强制跨域转移；
 * DEGRADED 域（部分端点故障）保留，容量受损但仍可承接流量。</p>
 *
 * <p>按域锁定流程：</p>
 * <ol>
 *   <li>收集候选实例的 channelId，通过 {@link ChannelGateway#findByIds} 批量取 {@link Channel#getClusterId}</li>
 *   <li>按 clusterId 分组，每组通过 {@link EndpointResolver} 按入站协议派生 endpointId</li>
 *   <li>调 {@link ClusterHealthAggregator#aggregate} 聚合域级健康状态</li>
 *   <li>过滤 DOWN 域的实例；DEGRADED/HEALTHY 域保留</li>
 * </ol>
 *
 * <p>就近路由（按 region 偏好择域）依赖请求级 region 上下文，当前 {@link RoutingRequest}
 * 未携带 region 字段，本任务暂不实现就近排序，仅实现「DOWN 域过滤」核心语义；
 * 就近择域待后续 task（4.9 画像贯穿）补充 RoutingRequest region 后实现。</p>
 *
 * <p>边界处理：</p>
 * <ul>
 *   <li>protocol 为 null：无法派生 endpointId 判断域健康，保守保留实例（不误杀）</li>
 *   <li>实例 channel 未关联 cluster（clusterId null）：不参与域聚合，保守保留</li>
 *   <li>endpoint 派生失败（无可用端点）：该实例无法归入任何域的健康判断，保守保留
 *       （熔断级过滤已由 {@link HealthRouter} 完成，此处不再二次过滤）</li>
 * </ul>
 *
 * <p>{@link #isForce()} 返回 false：DOWN 域过滤后若候选为空，让链继续而非终止，
 * 避免全部域恰好 DOWN 时直接返回空而错失后续 Router 的兜底机会。</p>
 */
@Component
@Order(250)
@RequiredArgsConstructor
public class ClusterAffinityRouter implements Router {

    private static final Logger log = LoggerFactory.getLogger(ClusterAffinityRouter.class);

    private final ChannelGateway channelGateway;
    private final ClusterHealthAggregator clusterHealthAggregator;
    private final EndpointResolver endpointResolver;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        Protocol protocol = request.getProtocol();
        if (protocol == null) {
            // 无法派生 endpointId 判断域健康，保守保留全部
            log.debug("入站协议为 null，ClusterAffinityRouter 保守保留全部 {} 个实例", instances.size());
            return instances;
        }

        // 1. 批量查 Channel 取 clusterId
        List<Long> channelIds = instances.stream().map(ModelInstance::getChannelId).toList();
        Map<Long, Long> channelToCluster = new HashMap<>();
        for (Channel ch : channelGateway.findByIds(channelIds)) {
            if (ch.getClusterId() != null) {
                channelToCluster.put(ch.getId(), ch.getClusterId());
            }
        }

        // 2. 按 clusterId 分组实例；clusterId 为 null 的实例不参与域聚合，直接保留
        Map<Long, List<ModelInstance>> byCluster = new HashMap<>();
        List<ModelInstance> noCluster = new ArrayList<>();
        Set<Long> downClusters = new HashSet<>();

        for (ModelInstance mi : instances) {
            Long clusterId = channelToCluster.get(mi.getChannelId());
            if (clusterId == null) {
                noCluster.add(mi);
                continue;
            }
            byCluster.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(mi);
        }

        // 3. 逐域派生 endpointId 并聚合健康状态，标记 DOWN 域
        for (Map.Entry<Long, List<ModelInstance>> entry : byCluster.entrySet()) {
            Long clusterId = entry.getKey();
            List<ModelInstance> clusterInstances = entry.getValue();

            List<Long> endpointIds = new ArrayList<>();
            boolean resolveFailed = false;
            for (ModelInstance mi : clusterInstances) {
                try {
                    ChannelEndpoint endpoint = endpointResolver.resolve(mi.getChannelId(), protocol);
                    endpointIds.add(endpoint.getId());
                } catch (Exception e) {
                    // 派生失败：该实例无法归入域健康判断，保守保留整个域（不判 DOWN）
                    log.debug("域 {} 内实例 channel {} 的 {} 协议端点派生失败，保守保留该域",
                            clusterId, mi.getChannelId(), protocol);
                    resolveFailed = true;
                    break;
                }
            }

            if (resolveFailed) {
                // 派生失败的域保留，不判 DOWN
                continue;
            }

            ClusterHealthStatus status = clusterHealthAggregator.aggregate(endpointIds);
            if (status == ClusterHealthStatus.DOWN) {
                log.debug("域 {} 整域 DOWN，过滤该域 {} 个实例，触发跨域转移",
                        clusterId, clusterInstances.size());
                downClusters.add(clusterId);
            }
        }

        // 4. 过滤 DOWN 域实例，保留其余（含 noCluster 与 DEGRADED/HEALTHY 域）
        List<ModelInstance> result = new ArrayList<>(noCluster);
        for (Map.Entry<Long, List<ModelInstance>> entry : byCluster.entrySet()) {
            if (!downClusters.contains(entry.getKey())) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public boolean isForce() {
        // DOWN 域过滤后若为空，让链继续而非终止，保留后续 Router 兜底机会
        return false;
    }
}
