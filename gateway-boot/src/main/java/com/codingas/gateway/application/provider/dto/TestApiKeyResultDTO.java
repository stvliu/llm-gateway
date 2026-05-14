package com.codingas.gateway.application.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * API Key 连通性测试结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestApiKeyResultDTO {

    /** 测试是否成功 */
    private boolean success;

    /** 结果消息 */
    private String message;

    /** 发现的模型列表（可选） */
    private List<String> models;
}