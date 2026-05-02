package com.codingas.gateway.application.dto.user;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {
    private String keyword;
    private UserStatus status;
    private String roleCode;
}
