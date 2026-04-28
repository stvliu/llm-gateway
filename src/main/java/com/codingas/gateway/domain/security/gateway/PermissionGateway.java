package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.Permission;
import java.util.List;
import java.util.Optional;

/**
 * 权限网关接口
 */
public interface PermissionGateway {

    /**
     * 保存权限
     */
    Permission save(Permission permission);

    /**
     * 根据 ID 查找权限
     */
    Optional<Permission> findById(Long id);

    /**
     * 根据权限编码查找权限
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * 查询所有权限
     */
    List<Permission> findAll();

    /**
     * 根据权限编码列表查询权限
     */
    List<Permission> findByPermissionCodes(List<String> permissionCodes);

    /**
     * 删除权限
     */
    void delete(Permission permission);
}
