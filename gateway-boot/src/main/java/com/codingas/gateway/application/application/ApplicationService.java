package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;

import java.util.List;

/**
 * 应用应用服务接口
 *
 * <p>管理应用聚合根的 CRUD 与渠道授权绑定。
 * Application 是权限+行为双聚合根，承载 Key 归属与渠道可见性。</p>
 */
public interface ApplicationService {

    /**
     * 创建应用
     *
     * @param request 创建请求（code/name/description）
     * @return 创建后的应用响应
     */
    ApplicationResponse create(ApplicationRequest request);

    /**
     * 更新应用
     *
     * @param id      应用 ID
     * @param request 更新请求
     * @return 更新后的应用响应
     */
    ApplicationResponse update(Long id, ApplicationRequest request);

    /**
     * 按主键查询应用
     *
     * @param id 应用 ID
     * @return 应用响应
     */
    ApplicationResponse getById(Long id);

    /**
     * 查询全部应用
     *
     * @return 应用响应列表
     */
    List<ApplicationResponse> getAll();

    /**
     * 删除应用（级联清理渠道授权关联）
     *
     * @param id 应用 ID
     */
    void delete(Long id);

    /**
     * 查询应用授权的渠道 ID 列表
     *
     * @param id 应用 ID
     * @return 渠道 ID 列表
     */
    List<Long> listChannelIds(Long id);

    /**
     * 更新应用渠道授权（先清空旧关联，再批量保存新关联）
     *
     * @param id         应用 ID
     * @param channelIds 渠道 ID 列表
     */
    void updateChannels(Long id, List<Long> channelIds);
}
