package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 渠道开通请求
 *
 * <p>支持批量创建 API Key 凭证，以及在 provider 不存在时通过 inlineProvider 内联创建供应商。</p>
 */
@Getter
@Setter
public class ProvisionRequest {

    /** API Key 列表（批量创建凭证） */
    private List<String> apiKeys;

    /**
     * 内联供应商信息（可选）
     *
     * <p>仅在目标 providerCode 尚未持久化时生效；若 providerCode 已存在，则该字段被忽略。</p>
     * <p>若提供，inlineProvider.code() 必须与套餐解析得到的 providerCode 一致，否则抛
     * {@code INLINE_PROVIDER_CODE_MISMATCH}。</p>
     */
    private InlineProvider inlineProvider;

    /**
     * 内联供应商参数
     *
     * @param code        供应商程序标识，必须与 planCode 解析出的 providerCode 一致
     * @param name        显示名（缺省时回退为 code）
     * @param description 描述
     * @param websiteUrl  官网 URL
     * @param apiDocUrl   API 文档 URL
     */
    public record InlineProvider(
            String code,
            String name,
            String description,
            String websiteUrl,
            String apiDocUrl
    ) {
    }
}
