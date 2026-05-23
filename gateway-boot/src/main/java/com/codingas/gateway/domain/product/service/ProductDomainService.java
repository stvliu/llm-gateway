package com.codingas.gateway.domain.product.service;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductModel;
import com.codingas.gateway.domain.product.gateway.ProductModelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产品领域服务
 * <p>
 * 封装产品的业务规则，保持实体仅含 Getter/Setter。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProductDomainService {

    private final ProductModelGateway productModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 判断产品是否包含指定模型
     * <p>
     * 通过 ProductModel 关联表查询，避免在 Product 实体中存储模型列表。
     * 使用批量查询避免 N+1 问题。
     * </p>
     *
     * @param product        产品实体
     * @param providerModelId 模型的 providerModelId
     * @return 是否包含该模型
     */
    public boolean containsModel(Product product, String providerModelId) {
        List<ProductModel> associations = productModelGateway.findByProductId(product.getId());
        if (associations.isEmpty()) {
            return false;
        }

        List<Long> modelIds = associations.stream()
            .map(ProductModel::getModelId)
            .collect(Collectors.toList());

        List<Model> models = modelGateway.findByIds(modelIds);
        return models.stream()
            .anyMatch(m -> providerModelId.equals(m.getProviderModelId()));
    }

    /**
     * 获取产品包含的所有模型
     */
    public List<Model> getModels(Product product) {
        List<ProductModel> associations = productModelGateway.findByProductId(product.getId());
        if (associations.isEmpty()) {
            return List.of();
        }

        List<Long> modelIds = associations.stream()
            .map(ProductModel::getModelId)
            .collect(Collectors.toList());

        return modelGateway.findByIds(modelIds);
    }

    /**
     * 获取产品的模型 providerModelId 集合
     */
    public Set<String> getModelIds(Product product) {
        return getModels(product).stream()
            .map(Model::getProviderModelId)
            .collect(Collectors.toSet());
    }
}