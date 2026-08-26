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
package com.codingas.gateway.iam.user;

import com.codingas.gateway.common.dto.PageResponse;

import java.util.List;

/**
 * 用户管理服务接口
 *
 * <p>处理用户管理的业务逻辑。出入参采用实体 {@link User} 与查询/结果对象，
 * HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface UserManager {

    /**
     * 创建用户
     *
     * @param user         用户实体（承载 username/email/phone/role）
     * @param plainPassword 明文密码（由本服务编码哈希后存储）
     * @return 创建后的用户实体
     */
    User create(User user, String plainPassword);

    /**
     * 根据 ID 获取用户
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    User getById(Long id);

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户实体分页
     */
    PageResponse<User> query(UserQuery query);

    /**
     * 更新用户（实体 null 字段不更新）
     *
     * @param id   用户 ID
     * @param user 用户实体（仅非 null 字段生效）
     * @return 更新后的用户实体
     */
    User update(Long id, User user);

    /**
     * 删除用户（软删除）
     *
     * @param id 用户 ID
     */
    void delete(Long id);

    /**
     * 更新用户状态
     *
     * @param id    用户 ID
     * @param state 目标状态
     * @return 更新后的用户实体
     */
    User updateState(Long id, UserState state);

    /**
     * 分配用户角色
     *
     * @param id        用户 ID
     * @param roleCodes 角色代码列表（取首个生效，空列表不修改）
     * @return 更新后的用户实体
     */
    User assignRoles(Long id, List<String> roleCodes);

    /**
     * 用户登录
     *
     * @param username   用户名
     * @param password   密码（明文）
     * @param rememberMe 是否记住登录态
     * @return 登录结果（用户实体、令牌与权限码）
     */
    LoginResult login(String username, String password, boolean rememberMe);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 修改密码
     *
     * @param userId          用户 ID
     * @param currentPassword 当前密码（明文）
     * @param newPassword     新密码（明文）
     */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * 重置密码（管理员触发）
     *
     * <p>生成 16 位随机密码（排除易混字符 O/0/I/1/l），更新哈希，
     * 一次性返回明文。禁止重置内建用户密码。</p>
     *
     * @param userId 用户 ID
     * @return 含一次性明文的用例结果
     */
    ResetPasswordResult resetPassword(Long userId);
}
