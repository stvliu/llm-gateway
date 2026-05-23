package com.codingas.gateway.domain.product.service;

import com.codingas.gateway.domain.product.entity.Product;
import org.springframework.stereotype.Service;

/**
 * 产品领域服务
 * <p>
 * 承载产品的业务逻辑，保持实体纯洁性（实体只含 Getter/Setter）。
 * </p>
 */
@Service
public class ProductDomainService {

    /**
     * 检查产品是否包含指定模型
     *
     * @param product   产品实体
     * @param modelName 模型名称
     * @return 是否包含
     */
    public boolean containsModel(Product product, String modelName) {
        if (product == null || product.getModels() == null) {
            return false;
        }
        return product.getModels().contains(modelName);
    }

    /**
     * 获取指定协议的端点
     *
     * @param product  产品实体
     * @param protocol 协议名称（如 "openai", "anthropic"）
     * @return 端点 URL，不存在返回 null
     */
    public String getEndpoint(Product product, String protocol) {
        if (product == null || product.getEndpoints() == null) {
            return null;
        }
        return product.getEndpoints().get(protocol);
    }

    /**
     * 获取默认端点
     * <p>
     * 优先返回 openai 协议端点，其次返回任意一个可用端点。
     * </p>
     *
     * @param product 产品实体
     * @return 默认端点 URL，不存在返回 null
     */
    public String getDefaultEndpoint(Product product) {
        if (product == null || product.getEndpoints() == null || product.getEndpoints().isEmpty()) {
            return null;
        }
        if (product.getEndpoints().containsKey("openai")) {
            return product.getEndpoints().get("openai");
        }
        return product.getEndpoints().values().iterator().next();
    }
}