package com.codingas.gateway.domain.security.entity;

/**
 * 权限常量定义
 */
public final class Permission {

    private Permission() {}

    // API 权限
    public static final String API_CALL = "api:call";
    public static final String API_READ = "api:read";
    public static final String API_WRITE = "api:write";

    // 模型权限
    public static final String MODEL_ACCESS = "model:access";
    public static final String MODEL_ADMIN = "model:admin";

    // 管理权限
    public static final String ADMIN_ALL = "*";
    public static final String USER_ADMIN = "admin:user";
    public static final String USER_MANAGE = "admin:manage";

    // 密钥权限
    public static final String KEY_CREATE = "key:create";
    public static final String KEY_READ = "key:read";
    public static final String KEY_UPDATE = "key:update";
    public static final String KEY_DELETE = "key:delete";
}
