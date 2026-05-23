package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.product.ProductService;
import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.enums.ProductType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getByProviderId(
            @RequestParam Long providerId,
            @RequestParam(required = false) String productType) {
        List<ProductResponse> responses;
        if (productType != null) {
            responses = productService.getByProviderIdAndType(
                providerId, ProductType.fromCode(productType));
        } else {
            responses = productService.getByProviderId(providerId);
        }
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
