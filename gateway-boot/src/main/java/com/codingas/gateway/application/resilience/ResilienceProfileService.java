package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ResilienceProfileRequest;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileResponse;

import java.util.List;

/**
 * 容灾画像应用服务接口
 *
 * <p>管理容灾画像聚合根的 CRUD（不提供 delete：default 画像为系统兜底禁删，
 * 其余画像因 {@code ResilienceProfileGateway} 无 delete 方法遵循既有模式）。
 * 委托 {@link com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway}。</p>
 */
public interface ResilienceProfileService {

    /**
     * 创建容灾画像
     *
     * @param request 创建请求
     * @return 创建后的画像响应
     */
    ResilienceProfileResponse create(ResilienceProfileRequest request);

    /**
     * 更新容灾画像
     *
     * @param id      画像 ID
     * @param request 更新请求
     * @return 更新后的画像响应
     */
    ResilienceProfileResponse update(Long id, ResilienceProfileRequest request);

    /**
     * 按主键查询容灾画像
     *
     * @param id 画像 ID
     * @return 画像响应
     */
    ResilienceProfileResponse getById(Long id);

    /**
     * 查询全部容灾画像
     *
     * @return 画像响应列表
     */
    List<ResilienceProfileResponse> getAll();
}
