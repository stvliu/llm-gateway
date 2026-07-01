package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationChannelItem;
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
     * 查询应用授权的渠道及其应用级转移优先级
     *
     * <p>Task gap2：转移顺序由应用级 {@code ApplicationChannel.priority} 决定，
     * 管理端需读取 priority 配置。返回列表每项含 channelId 与 priority。</p>
     *
     * @param id 应用 ID
     * @return 渠道授权项列表（channelId + priority）
     */
    List<ApplicationChannelItem> listChannels(Long id);

    /**
     * 更新应用渠道授权（先清空旧关联，再批量保存含 priority 的新关联）
     *
     * <p>Task gap2：用三参构造器 {@code ApplicationChannel(appId, channelId, priority)}
     * 保存，使 priority 写入应用-渠道关联实体。</p>
     *
     * @param id       应用 ID
     * @param channels 渠道授权项列表（channelId + priority；空列表表示清空全部授权）
     */
    void updateChannels(Long id, List<ApplicationChannelItem> channels);
}
