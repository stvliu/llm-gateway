package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.common.util.JsonUtils;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.infrastructure.metadata.database.ProductMetadataDo;
import com.codingas.gateway.infrastructure.metadata.database.ProductMetadataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 产品元数据网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetadataGatewayImpl implements ProductMetadataGateway {

    private final ProductMetadataRepository repository;

    @Override
    public ProductMetadata save(ProductMetadata metadata) {
        ProductMetadataDo doEntity = toDo(metadata);
        ProductMetadataDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductMetadata> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductMetadata> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProductMetadata> findByProviderId(String providerId) {
        return repository.findByProviderId(providerId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public Optional<ProductMetadata> findByProviderIdAndProductName(String providerId, String productName) {
        return repository.findByProviderIdAndProductName(providerId, productName)
                .map(this::toEntity);
    }

    @Override
    public Optional<ProductMetadata> findDefaultByProviderId(String providerId) {
        return repository.findByProviderIdAndIsDefaultTrue(providerId)
                .map(this::toEntity);
    }

    @Override
    public List<ProductMetadata> saveAll(List<ProductMetadata> metadataList) {
        List<ProductMetadataDo> doList = metadataList.stream().map(this::toDo).toList();
        return repository.saveAll(doList).stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndProductName(String providerId, String productName) {
        return repository.existsByProviderIdAndProductName(providerId, productName);
    }

    // ==================== DO ↔ Entity 转换 ====================

    private ProductMetadata toEntity(ProductMetadataDo doEntity) {
        ProductMetadata entity = new ProductMetadata();
        entity.setId(doEntity.getId());
        entity.setProviderId(doEntity.getProviderId());
        entity.setProductName(doEntity.getProductName());
        entity.setProductType(parseProductType(doEntity.getProductType()));
        entity.setDescription(doEntity.getDescription());
        entity.setEndpoints(parseEndpoints(doEntity.getEndpoints()));
        entity.setIsDefault(doEntity.getIsDefault());
        entity.setState(parseMetadataState(doEntity.getState()));
        entity.setSource(parseMetadataSource(doEntity.getSource()));
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());
        return entity;
    }

    private ProductMetadataDo toDo(ProductMetadata entity) {
        ProductMetadataDo doEntity = new ProductMetadataDo();
        doEntity.setId(entity.getId());
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setProductName(entity.getProductName());
        doEntity.setProductType(entity.getProductType() != null ? entity.getProductType().name() : ProductType.STANDARD.name());
        doEntity.setDescription(entity.getDescription());
        doEntity.setEndpoints(toJson(entity.getEndpoints()));
        doEntity.setIsDefault(entity.getIsDefault() != null ? entity.getIsDefault() : false);
        doEntity.setState(entity.getState() != null ? entity.getState().name() : MetadataState.ACTIVE.name());
        doEntity.setSource(entity.getSource() != null ? entity.getSource().name() : MetadataSource.BUILTIN.name());
        doEntity.setCreatedAt(entity.getCreatedAt());
        doEntity.setCreatedBy(entity.getCreatedBy());
        doEntity.setUpdatedAt(entity.getUpdatedAt());
        doEntity.setUpdatedBy(entity.getUpdatedBy());
        return doEntity;
    }

    private Map<String, String> parseEndpoints(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            // 处理双重序列化：如果 JSON 是字符串形式（被再次序列化），先解包
            String actualJson = json;
            if (json.startsWith("\"") && json.endsWith("\"")) {
                // 双重序列化的情况，先解包一层
                actualJson = JsonUtils.fromJson(json, new TypeReference<String>() {});
            }
            return JsonUtils.fromJson(actualJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Endpoints JSON 解析失败: {}", json, e);
            return Map.of();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        return JsonUtils.toJson(obj);
    }

    private ProductType parseProductType(String value) {
        if (value == null || value.isBlank()) return ProductType.STANDARD;
        try {
            return ProductType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ProductType value: {}, defaulting to STANDARD", value);
            return ProductType.STANDARD;
        }
    }

    private MetadataState parseMetadataState(String value) {
        if (value == null || value.isBlank()) return MetadataState.ACTIVE;
        try {
            return MetadataState.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid MetadataState value: {}, defaulting to ACTIVE", value);
            return MetadataState.ACTIVE;
        }
    }

    private MetadataSource parseMetadataSource(String value) {
        if (value == null || value.isBlank()) return MetadataSource.BUILTIN;
        try {
            return MetadataSource.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid MetadataSource value: {}, defaulting to BUILTIN", value);
            return MetadataSource.BUILTIN;
        }
    }
}
