package com.codingas.gateway.domain.metadata.service;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据领域服务
 * <p>
 * 封装产品元数据的业务规则，保持实体仅含 Getter/Setter。
 * </p>
 */
@Service
public class ProductMetadataDomainService {

    /**
     * 从内置数据创建产品元数据
     * <p>
     * 用于从 classpath JSON 文件加载内置产品元数据。
     * </p>
     *
     * @param providerId  供应商 ID
     * @param productName 产品名称
     * @param productType 产品类型
     * @param endpoints   端点映射（协议 -> base_url）
     * @param description 产品描述
     * @param isDefault   是否为该供应商的默认产品
     * @return 新创建的产品元数据实体
     */
    public ProductMetadata createFromBuiltinData(
            String providerId,
            String productName,
            ProductType productType,
            Map<String, String> endpoints,
            String description,
            Boolean isDefault) {

        ProductMetadata metadata = new ProductMetadata(providerId, productName, productType);
        metadata.setEndpoints(endpoints);
        metadata.setDescription(description);
        metadata.setIsDefault(isDefault != null ? isDefault : false);
        metadata.setState(MetadataState.ACTIVE);
        metadata.setSource(MetadataSource.BUILTIN);
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());
        return metadata;
    }

    /**
     * 应用更新数据
     * <p>
     * 仅更新非空字段，保持已有值不变。
     * </p>
     *
     * @param existing    已存在的产品元数据
     * @param endpoints   新的端点映射（可为 null）
     * @param description 新的描述（可为 null）
     * @param isDefault   新的默认标记（可为 null）
     */
    public void applyUpdateData(ProductMetadata existing, Map<String, String> endpoints,
                                 String description, Boolean isDefault) {
        if (endpoints != null) {
            existing.setEndpoints(endpoints);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (isDefault != null) {
            existing.setIsDefault(isDefault);
        }
        existing.setUpdatedAt(Instant.now());
    }

    /**
     * 检查是否可被同步覆盖
     * <p>
     * 仅 BUILTIN 来源的记录可被外部同步更新，
     * MANUAL 和 OVERRIDE 的记录不会被覆盖。
     * </p>
     *
     * @param metadata 产品元数据
     * @return 是否可被同步覆盖
     */
    public boolean canBeOverriddenBySync(ProductMetadata metadata) {
        return metadata.getSource() == MetadataSource.BUILTIN;
    }

    /**
     * 标记为已废弃（上游数据源中消失）
     *
     * @param metadata 产品元数据
     */
    public void markDeprecated(ProductMetadata metadata) {
        metadata.setState(MetadataState.DEPRECATED);
        metadata.setUpdatedAt(Instant.now());
    }
}
