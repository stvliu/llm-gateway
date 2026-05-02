package com.codingas.gateway.application.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 用户角色分配请求
 *
 * <p>简化角色模型：仅支持分配单一角色（取列表第一个）。</p>
 */
@Data
public class UserRoleAssignRequest {
    /**
     * 角色代码列表（简化模型下仅使用第一个）
     */
    @NotEmpty(message = "角色代码不能为空")
    private List<String> roleCodes;
}
