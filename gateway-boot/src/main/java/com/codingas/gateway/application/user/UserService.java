package com.codingas.gateway.application.user;

import com.codingas.gateway.application.auth.dto.ChangePasswordRequest;
import com.codingas.gateway.application.auth.dto.LoginRequest;
import com.codingas.gateway.application.auth.dto.LoginResponse;
import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * 用户应用服务接口
 *
 * <p>处理用户管理的业务逻辑。</p>
 */
public interface UserService {

    /**
     * 创建用户
     */
    UserResponse create(UserCreateRequest request);

    /**
     * 根据 ID 获取用户
     */
    UserResponse getById(Long id);

    /**
     * 查询用户列表
     */
    PageResponse<UserResponse> query(UserQueryRequest request);

    /**
     * 更新用户
     */
    UserResponse update(Long id, UserUpdateRequest request);

    /**
     * 删除用户（软删除）
     */
    void delete(Long id);

    /**
     * 更新用户状态
     */
    UserResponse updateState(Long id, UserStateUpdateRequest request);

    /**
     * 分配用户角色
     */
    UserResponse assignRoles(Long id, UserRoleAssignRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含用户信息和令牌）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 重置密码（管理员触发）
     *
     * <p>生成 16 位随机密码（排除易混字符 O/0/I/1/l），更新哈希，
     * 一次性返回明文。禁止重置内建用户密码。</p>
     *
     * @param userId 用户 ID
     * @return 含一次性明文的响应
     */
    ResetPasswordResponse resetPassword(Long userId);
}
