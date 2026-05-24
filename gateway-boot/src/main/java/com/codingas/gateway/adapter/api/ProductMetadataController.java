package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.metadata.ProductMetadataService;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProductMetadataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道元数据 API
 */
@RestController
@RequestMapping("/api/v1/product-metadata")
@RequiredArgsConstructor
public class ProductMetadataController {

    private final ProductMetadataService productMetadataService;

    /**
     * 查询渠道元数据列表
     */
    @GetMapping
    public ResponseEntity<List<ProductMetadataResponse>> list(
            @RequestParam(required = false) String providerId) {
        List<ProductMetadataResponse> list = productMetadataService.listProductMetadata(providerId);
        return ResponseEntity.ok(list);
    }

    /**
     * 获取渠道元数据详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductMetadataResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productMetadataService.getProductMetadata(id));
    }

    /**
     * 查询某供应商的所有渠道
     */
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ProductMetadataResponse>> listByProviderId(
            @PathVariable String providerId) {
        return ResponseEntity.ok(productMetadataService.listProductMetadata(providerId));
    }

    /**
     * 创建渠道元数据
     */
    @PostMapping
    public ResponseEntity<ProductMetadataResponse> create(
            @Valid @RequestBody MetadataCreateRequest request) {
        return ResponseEntity.ok(productMetadataService.createMetadata(request));
    }

    /**
     * 更新渠道元数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductMetadataResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MetadataUpdateRequest request) {
        return ResponseEntity.ok(productMetadataService.updateMetadata(id, request));
    }

    /**
     * 删除渠道元数据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productMetadataService.deleteMetadata(id);
        return ResponseEntity.noContent().build();
    }
}