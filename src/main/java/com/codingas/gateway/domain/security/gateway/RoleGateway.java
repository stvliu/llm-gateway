package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.Role;
import java.util.List;
import java.util.Optional;

/**
 * 角色网关接口
 */
public interface RoleGateway {

    /**
     * 保存角色
     */
    Role save(Role role);

    /**
     * 根据 ID 查找角色
     */
    Optional<Role> findById(Long id);

    /**
     * 根据角色编码查找角色
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 查询所有角色
     */
    List<Role> findAll();

    /**
     * 根据角色编码列表查询角色
     */
    List<Role> findByRoleCodes(List<String> roleCodes);

    /**
     * 统计角色总数
     */
    long count();

    /**
     * 删除角色
     */
    void delete(Role role);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @return 是否存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 保存用户角色关联
     *
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     */
    void saveUserRoles(Long userId, List<Long> roleIds);
}
