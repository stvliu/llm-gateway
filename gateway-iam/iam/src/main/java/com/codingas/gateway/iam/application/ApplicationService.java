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
package com.codingas.gateway.iam.application;

import java.util.List;

/**
 * 应用管理服务接口
 *
 * <p>管理应用根实体的 CRUD 与渠道授权绑定。
 * Application 是权限+行为双根实体，承载 Key 归属与渠道可见性。</p>
 *
 * <p>出入参采用实体与轻量用例对象，HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface ApplicationService {

    /**
     * 创建应用
     *
     * @param app 应用实体（承载 code/name/description/timeout/failureStrategy）
     * @return 创建后的应用实体
     */
    Application create(Application app);

    /**
     * 更新应用
     *
     * @param id  应用 ID
     * @param app 应用实体
     * @return 更新后的应用实体
     */
    Application update(Long id, Application app);

    /**
     * 按主键查询应用
     *
     * @param id 应用 ID
     * @return 应用实体
     */
    Application getById(Long id);

    /**
     * 查询全部应用
     *
     * @return 应用实体列表
     */
    List<Application> getAll();

    /**
     * 删除应用（级联清理渠道授权关联）
     *
     * @param id 应用 ID
     */
    void delete(Long id);

    /**
     * 查询应用授权的渠道及其应用级转移优先级
     *
     * <p>转移顺序由应用级 {@code ApplicationChannel.priority} 决定，
     * 管理端需读取 priority 配置。返回列表每项含 channelId 与 priority。</p>
     *
     * @param id 应用 ID
     * @return 渠道授权关联实体列表（channelId + priority）
     */
    List<ApplicationChannel> listChannels(Long id);

    /**
     * 更新应用渠道授权（先清空旧关联，再批量保存含 priority 的新关联）
     *
     * @param id       应用 ID
     * @param channels 渠道授权项用例入参列表（channelId + priority；空列表表示清空全部授权）
     */
    void updateChannels(Long id, List<ApplicationChannel> channels);
}
