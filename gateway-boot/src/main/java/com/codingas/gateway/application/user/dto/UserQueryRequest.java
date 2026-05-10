package com.codingas.gateway.application.user.dto;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.security.enums.UserState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {
    private String keyword;
    private UserState state;
    private String roleCode;
}
