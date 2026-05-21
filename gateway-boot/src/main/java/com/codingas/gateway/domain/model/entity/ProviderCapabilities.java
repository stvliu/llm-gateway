package com.codingas.gateway.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 供应商能力描述
 *
 * <p>描述供应商支持的协议和功能。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCapabilities {

    /** 是否支持 OpenAI Chat Completions 格式 */
    private boolean supportsChatCompletion;

    /** 是否支持 Anthropic Messages 格式 */
    private boolean supportsMessages;

    /** 是否支持 Embeddings */
    private boolean supportsEmbeddings;

    /** 是否支持流式 */
    private boolean supportsStreaming;

    /** 是否支持函数调用 */
    private boolean supportsFunctionCalling;

    /** 支持的模型列表 */
    private Set<String> supportedModels;
}