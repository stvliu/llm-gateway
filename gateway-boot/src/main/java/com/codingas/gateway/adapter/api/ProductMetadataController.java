package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.ProductMetadataService;
import com.codingas.gateway.application.metadata.dto.ProductMetadataResponse;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品元数据 API
 */
@RestController
@RequestMapping("/api/v1/product-metadata")
@RequiredArgsConstructor
public class ProductMetadataController {

    private final ProductMetadataService productMetadataService;

    /**
     * 分页查询产品元数据
     */
    @GetMapping
    public ResponseEntity<Page<ProductMetadataResponse>> list(
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String productType,
            @PageableDefault(size = 20) Pageable pageable) {
        ProductType type = productType != null ? ProductType.valueOf(productType) : null;
        Page<ProductMetadataResponse> page = productMetadataService.listProductMetadata(
            providerId, type, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 获取产品元数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductMetadataResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productMetadataService.getProductMetadata(id));
    }

    /**
     * 查询某供应商的所有产品
     */
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ProductMetadataResponse>> listByProviderId(
            @PathVariable String providerId) {
        return ResponseEntity.ok(productMetadataService.listByProviderId(providerId));
    }

    /**
     * 删除产品元数据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productMetadataService.deleteProductMetadata(id);
        return ResponseEntity.noContent().build();
    }
}