package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模型规格查询请求
 */
@Data
public class ModelSpecQueryRequest {

    @Size(max = 128, message = "关键词长度不能超过 128")
    private String keyword;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态值只能是 ACTIVE 或 INACTIVE")
    private String state;
}
