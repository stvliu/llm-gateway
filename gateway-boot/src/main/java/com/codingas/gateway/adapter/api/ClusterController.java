package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.resilience.ClusterService;
import com.codingas.gateway.application.resilience.dto.ClusterRequest;
import com.codingas.gateway.application.resilience.dto.ClusterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 故障域管理 REST 控制器
 *
 * <p>提供 Cluster 故障域聚合根的 create/update/get/list API。
 * 不提供 delete：Cluster 关联 Channel 的 clusterId，删除需级联清理；且 Gateway 无 delete 方法。</p>
 *
 * <p>Cluster 是 Channel 的<b>跨供应商故障独立性分组</b>，同组 Channel 共享共因特征
 * （同供应商/同账号/同专线等），整组故障才跨组转移。Cluster 与 providerId 共存正交
 * （spec cluster-failover）。API 路径 {@code /api/v1/resilience/clusters} 保持不变。</p>
 */
@RestController
@RequestMapping("/api/v1/resilience/clusters")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    /**
     * 创建故障域
     *
     * @param request 创建请求
     * @return 创建后的故障域响应
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClusterResponse create(@Valid @RequestBody ClusterRequest request) {
        return clusterService.create(request);
    }

    /**
     * 更新故障域
     *
     * @param id      故障域 ID
     * @param request 更新请求
     * @return 更新后的故障域响应
     */
    @PutMapping("/{id}")
    public ClusterResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ClusterRequest request) {
        return clusterService.update(id, request);
    }

    /**
     * 查询故障域详情
     *
     * @param id 故障域 ID
     * @return 故障域响应
     */
    @GetMapping("/{id}")
    public ClusterResponse getById(@PathVariable Long id) {
        return clusterService.getById(id);
    }

    /**
     * 查询全部故障域列表
     *
     * @return 故障域响应列表
     */
    @GetMapping
    public List<ClusterResponse> list() {
        return clusterService.getAll();
    }
}
