package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ClusterRequest;
import com.codingas.gateway.application.resilience.dto.ClusterResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.domain.resilience.gateway.ClusterGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 故障域应用服务实现
 *
 * <p>管理 Cluster 故障域聚合根的 create/update/get/list。
 * 委托 {@link ClusterGateway}，code 全局唯一校验。</p>
 *
 * <p><b>Task 6 变更</b>：Cluster 字段瘦身为 code/name/description/providerId + 审计，
 * 删除 region/priority/healthStatus 处理；create 不再设置默认 healthStatus
 * （域级健康聚合随 DomainHealth 路由器在 Task 5 移除）。</p>
 *
 * <p>不提供 delete：Cluster 关联 Channel 的 clusterId，删除需级联清理；
 * 且 {@code ClusterGateway} 无 delete 方法，遵循既有模式不新增。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterServiceImpl implements ClusterService {

    private final ClusterGateway clusterGateway;

    @Override
    @Transactional
    public ClusterResponse create(ClusterRequest request) {
        // code 全局唯一校验
        if (clusterGateway.findByCode(request.getCode()) != null) {
            throw new GatewayRequestException("CLUSTER_CODE_DUPLICATE",
                    "故障域编码已存在: " + request.getCode());
        }

        Cluster cluster = new Cluster();
        applyRequestToEntity(cluster, request);

        Cluster saved = clusterGateway.save(cluster);
        log.info("Created cluster: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ClusterResponse update(Long id, ClusterRequest request) {
        Cluster cluster = clusterGateway.findById(id);
        if (cluster == null) {
            throw new GatewayRequestException("CLUSTER_NOT_FOUND",
                    "故障域不存在: " + id);
        }

        // code 变更时校验新 code 不与其他故障域冲突
        if (!cluster.getCode().equals(request.getCode())) {
            if (clusterGateway.findByCode(request.getCode()) != null) {
                throw new GatewayRequestException("CLUSTER_CODE_DUPLICATE",
                        "故障域编码已存在: " + request.getCode());
            }
        }

        applyRequestToEntity(cluster, request);

        Cluster saved = clusterGateway.save(cluster);
        log.info("Updated cluster: id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public ClusterResponse getById(Long id) {
        Cluster cluster = clusterGateway.findById(id);
        if (cluster == null) {
            throw new GatewayRequestException("CLUSTER_NOT_FOUND",
                    "故障域不存在: " + id);
        }
        return toResponse(cluster);
    }

    @Override
    public List<ClusterResponse> getAll() {
        return clusterGateway.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将请求 DTO 字段应用到实体
     */
    private void applyRequestToEntity(Cluster cluster, ClusterRequest request) {
        cluster.setCode(request.getCode());
        cluster.setName(request.getName());
        cluster.setProviderId(request.getProviderId());
        cluster.setDescription(request.getDescription());
    }

    /**
     * 实体转响应 DTO
     */
    private ClusterResponse toResponse(Cluster cluster) {
        ClusterResponse response = new ClusterResponse();
        response.setId(cluster.getId());
        response.setCode(cluster.getCode());
        response.setName(cluster.getName());
        response.setProviderId(cluster.getProviderId());
        response.setDescription(cluster.getDescription());
        response.setCreatedAt(cluster.getCreatedAt());
        response.setUpdatedAt(cluster.getUpdatedAt());
        return response;
    }
}
