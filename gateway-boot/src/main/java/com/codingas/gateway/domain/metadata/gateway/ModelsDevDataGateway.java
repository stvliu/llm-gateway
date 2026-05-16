package com.codingas.gateway.domain.metadata.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Models.dev 数据获取网关
 * <p>
 * 防腐层：隔离外部 API 变化对领域层的影响。
 * 纯数据获取，不包含业务逻辑。
 * </p>
 */
public interface ModelsDevDataGateway {

    /**
     * 获取所有支持的供应商的模型数据
     * @return Map&lt;providerId, List&lt;ModelData&gt;&gt;
     */
    Map<String, List<ModelData>> fetchAllSupportedModels();

    /**
     * 外部模型数据载体
     * <p>
     * 纯数据结构，无业务逻辑，用于领域层处理。
     * </p>
     */
    record ModelData(
        String modelId,
        String displayName,
        BigDecimal inputPrice,
        BigDecimal outputPrice,
        BigDecimal reasoningPrice,
        BigDecimal cacheReadPrice,
        BigDecimal cacheWritePrice,
        BigDecimal inputAudioPrice,
        BigDecimal outputAudioPrice,
        Integer contextWindow,
        Integer maxInputTokens,
        Integer maxOutputTokens,
        String knowledgeCutoff,
        Boolean openWeights,
        String family,
        Boolean vision,
        Boolean functionCalling
    ) {}
}
