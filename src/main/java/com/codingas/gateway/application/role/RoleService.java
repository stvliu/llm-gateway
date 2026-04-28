package com.codingas.gateway.application.role;

import com.codingas.gateway.adapter.admin.dto.role.RoleCreateRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleQueryRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleResponse;
import com.codingas.gateway.adapter.admin.dto.role.RoleUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

/**
 * 角色应用服务接口
 *
 * <p>处理角色管理的业务逻辑。</p>
 */
public interface RoleService {

    /**
     * 创建角色
     */
    RoleResponse create(RoleCreateRequest request);

    /**
     * 根据 ID 获取角色
     */
    RoleResponse getById(Long id);

    /**
     * 查询角色列表
     */
    PageResponse<RoleResponse> query(RoleQueryRequest request);

    /**
     * 更新角色
     */
    RoleResponse update(Long id, RoleUpdateRequest request);

    /**
     * 删除角色（软删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用角色
     */
    RoleResponse setEnabled(Long id, boolean enabled);
}