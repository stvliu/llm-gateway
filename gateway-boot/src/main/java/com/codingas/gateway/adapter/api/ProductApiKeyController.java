package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.productapikey.ProductApiKeyService;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品 API Key 管理控制器
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/api-keys")
@RequiredArgsConstructor
public class ProductApiKeyController {

    private final ProductApiKeyService productApiKeyService;

    /**
     * 获取产品下的 API Key 列表
     */
    @GetMapping
    public List<ProductApiKeyResponse> list(@PathVariable Long productId) {
        return productApiKeyService.listByProductId(productId);
    }

    /**
     * 根据 ID 获取 API Key 详情（含明文，用于页面复制）
     */
    @GetMapping("/{id}")
    public ProductApiKeyDetailResponse get(
            @PathVariable Long productId,
            @PathVariable Long id) {
        return productApiKeyService.getDetailById(productId, id);
    }

    /**
     * 创建产品 API Key
     */
    @PostMapping
    public ProductApiKeyCreateResponse create(
            @PathVariable Long productId,
            @Valid @RequestBody ProductApiKeyCreateRequest request) {
        return productApiKeyService.create(productId, request);
    }

    /**
     * 更新产品 API Key
     */
    @PutMapping("/{id}")
    public ProductApiKeyResponse update(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody ProductApiKeyUpdateRequest request) {
        return productApiKeyService.update(productId, id, request);
    }

    /**
     * 删除产品 API Key
     */
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long productId,
            @PathVariable Long id) {
        productApiKeyService.delete(productId, id);
    }
}