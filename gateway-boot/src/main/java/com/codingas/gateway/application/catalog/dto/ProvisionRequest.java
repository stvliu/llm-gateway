package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 渠道开通请求
 *
 * <p>支持批量创建 API Key 凭证。</p>
 */
@Getter
@Setter
public class ProvisionRequest {

    /** API Key 列表（批量创建凭证） */
    private List<String> apiKeys;
}