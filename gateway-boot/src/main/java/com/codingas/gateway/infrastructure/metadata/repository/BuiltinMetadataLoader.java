package com.codingas.gateway.infrastructure.metadata.repository;

import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内置元数据资源加载器
 * <p>
 * 从 classpath:metadata/providers/、classpath:metadata/products/ 和 classpath:metadata/models/ 目录
 * 分别加载供应商元数据、产品元数据和模型元数据。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinMetadataLoader {

    private static final String PROVIDERS_LOCATION = "classpath*:metadata/providers/*.json";
    private static final String PRODUCTS_LOCATION = "classpath*:metadata/products/*.json";
    private static final String MODELS_LOCATION = "classpath*:metadata/models/*.json";
    private static final String PRODUCT_MODELS_LOCATION = "classpath*:metadata/product-models/*.json";

    private final ResourceLoader resourceLoader;

    /**
     * 加载所有内置供应商元数据
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadProviderMetadata() {
        return loadFromLocation(PROVIDERS_LOCATION);
    }

    /**
     * 加载所有内置产品元数据
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadProductMetadata() {
        List<Map<String, Object>> allProducts = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(PRODUCTS_LOCATION);
            log.info("Found {} builtin product metadata files", resources.length);

            for (Resource resource : resources) {
                try {
                    // 产品元数据 JSON 是数组格式
                    List<Map<String, Object>> products = JsonUtils.fromJson(
                        resource.getInputStream(),
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    // 从文件名推断 provider_id
                    String filename = resource.getFilename();
                    String providerId = filename != null ? filename.replace(".json", "") : null;
                    if (providerId != null) {
                        for (Map<String, Object> product : products) {
                            // 如果 JSON 中没有 provider_id，从文件名推断
                            if (!product.containsKey("provider_id")) {
                                product.put("provider_id", providerId);
                            }
                        }
                    }
                    allProducts.addAll(products);
                } catch (Exception e) {
                    log.error("Failed to load product metadata from: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to resolve product metadata resources from classpath", e);
        }
        return allProducts;
    }

    /**
     * 加载所有内置模型元数据
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadModelMetadata() {
        List<Map<String, Object>> allModels = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(MODELS_LOCATION);
            log.info("Found {} builtin model metadata files", resources.length);

            for (Resource resource : resources) {
                try {
                    List<Map<String, Object>> models = JsonUtils.fromJson(
                        resource.getInputStream(),
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    String filename = resource.getFilename();
                    String providerId = filename != null ? filename.replace(".json", "") : null;
                    if (providerId != null) {
                        for (Map<String, Object> model : models) {
                            model.put("provider_id", providerId);
                        }
                    }
                    allModels.addAll(models);
                } catch (Exception e) {
                    log.error("Failed to load model metadata from: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to resolve model metadata resources from classpath", e);
        }
        return allModels;
    }

    /**
     * 加载所有内置产品-模型关联元数据
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadProductModelMetadata() {
        List<Map<String, Object>> allAssociations = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(PRODUCT_MODELS_LOCATION);
            log.info("Found {} builtin product-model metadata files", resources.length);

            for (Resource resource : resources) {
                try {
                    List<Map<String, Object>> associations = JsonUtils.fromJson(
                        resource.getInputStream(),
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    String filename = resource.getFilename();
                    String providerId = filename != null ? filename.replace(".json", "") : null;
                    if (providerId != null) {
                        for (Map<String, Object> assoc : associations) {
                            if (!assoc.containsKey("provider_id")) {
                                assoc.put("provider_id", providerId);
                            }
                        }
                    }
                    allAssociations.addAll(associations);
                } catch (Exception e) {
                    log.error("Failed to load product-model metadata from: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to resolve product-model metadata resources from classpath", e);
        }
        return allAssociations;
    }

    /**
     * 从指定位置加载 JSON 资源
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadFromLocation(String location) {
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(location);
            log.info("Found {} builtin metadata files from {}", resources.length, location);

            for (Resource resource : resources) {
                try {
                    Map<String, Object> item = JsonUtils.fromJson(
                        resource.getInputStream(),
                        new TypeReference<Map<String, Object>>() {}
                    );
                    items.add(item);
                    log.debug("Loaded builtin metadata: {} from {}", item.get("provider_id"), resource.getFilename());
                } catch (Exception e) {
                    log.error("Failed to load metadata from: {}", resource.getFilename(), e);
                }
            }
            log.info("Successfully loaded {} builtin metadata items", items.size());
        } catch (IOException e) {
            log.warn("Failed to resolve metadata resources from classpath", e);
        }
        return items;
    }
}