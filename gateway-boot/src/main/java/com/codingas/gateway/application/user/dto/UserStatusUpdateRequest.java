package com.codingas.gateway.application.user.dto;

import com.codingas.gateway.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求
 */
@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "状态不能为空")
    private UserStatus status;
}
