package com.codingas.gateway.application.productapikey;

import com.codingas.gateway.application.product.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;

import java.util.List;

/**
 * 产品 API Key 应用服务接口
 */
public interface ProductApiKeyService {

    /**
     * 创建产品 API Key
     */
    ProductApiKeyCreateResponse create(Long productId, ProductApiKeyCreateRequest request);

    /**
     * 根据 ID 获取产品 API Key（校验产品归属，不含明文）
     */
    ProductApiKeyResponse getById(Long productId, Long id);

    /**
     * 根据 ID 获取产品 API Key 详情（含明文，用于页面复制）
     */
    ProductApiKeyDetailResponse getDetailById(Long productId, Long id);

    /**
     * 获取产品下的所有 API Key
     */
    List<ProductApiKeyResponse> listByProductId(Long productId);

    /**
     * 更新产品 API Key（校验产品归属）
     */
    ProductApiKeyResponse update(Long productId, Long id, ProductApiKeyUpdateRequest request);

    /**
     * 删除产品 API Key（校验产品归属）
     */
    void delete(Long productId, Long id);

    /**
     * 测试 API Key 是否有效
     */
    ApiKeyTestResponse testApiKey(Long productId, Long id);
}
