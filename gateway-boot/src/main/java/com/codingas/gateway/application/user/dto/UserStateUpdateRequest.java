package com.codingas.gateway.application.user.dto;

import com.codingas.gateway.domain.iam.enums.UserState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求
 */
@Data
public class UserStateUpdateRequest {
    @NotNull(message = "状态不能为空")
    private UserState state;
}
