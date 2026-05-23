package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ProductMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetadataService {

    private final ProductMetadataGateway productMetadataGateway;

    /**
     * 分页查询产品元数据
     */
    public Page<ProductMetadataResponse> listProductMetadata(
            String providerId, ProductType productType, Pageable pageable) {
        List<ProductMetadata> allProducts;

        if (providerId != null) {
            allProducts = productMetadataGateway.findByProviderId(providerId);
        } else {
            allProducts = productMetadataGateway.findAll();
        }

        // 按产品类型筛选
        if (productType != null) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getProductType() == productType)
                    .toList();
        }

        // 手动分页（注意边界检查）
        int start = (int) pageable.getOffset();
        if (start >= allProducts.size()) {
            // 超出范围时返回空页
            return new PageImpl<>(List.of(), pageable, allProducts.size());
        }
        int end = Math.min(start + pageable.getPageSize(), allProducts.size());
        List<ProductMetadata> pageContent = allProducts.subList(start, end);

        return new PageImpl<>(pageContent.stream().map(this::toResponse).toList(), pageable, allProducts.size());
    }

    /**
     * 获取产品元数据详情
     */
    public ProductMetadataResponse getProductMetadata(Long id) {
        ProductMetadata metadata = productMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("产品元数据不存在: id=" + id));
        return toResponse(metadata);
    }

    /**
     * 查询某供应商的所有产品
     */
    public List<ProductMetadataResponse> listByProviderId(String providerId) {
        return productMetadataGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 删除产品元数据
     */
    @Transactional
    public void deleteProductMetadata(Long id) {
        productMetadataGateway.deleteById(id);
        log.info("Deleted product metadata: id={}", id);
    }

    private ProductMetadataResponse toResponse(ProductMetadata metadata) {
        return ProductMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .productName(metadata.getProductName())
            .productType(metadata.getProductType() != null ? metadata.getProductType().name() : null)
            .description(metadata.getDescription())
            .endpoints(metadata.getEndpoints())
            .isDefault(metadata.getIsDefault())
            .inputPrice(metadata.getInputPrice())
            .outputPrice(metadata.getOutputPrice())
            .reasoningPrice(metadata.getReasoningPrice())
            .cacheReadPrice(metadata.getCacheReadPrice())
            .cacheWritePrice(metadata.getCacheWritePrice())
            .inputAudioPrice(metadata.getInputAudioPrice())
            .outputAudioPrice(metadata.getOutputAudioPrice())
            .state(metadata.getState() != null ? metadata.getState().name() : null)
            .source(metadata.getSource() != null ? metadata.getSource().name() : null)
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .build();
    }
}