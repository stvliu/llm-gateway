package com.codingas.gateway.application.product;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 渠道应用服务实现
 *
 * <p>管理渠道（Channel）的 CRUD 操作。</p>
 * <p>原 Product 实体已迁移为 Channel，原 ProductType 映射为 BillingMode。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ChannelGateway channelGateway;
    private final ProviderGateway providerGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
            throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
        }

        Channel channel = new Channel();
        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getProductType()));
        channel.setEndpoints(toJsonString(request.getEndpoints()));
        channel.setInputPrice(request.getInputPrice());
        channel.setOutputPrice(request.getOutputPrice());
        channel.setReasoningPrice(request.getReasoningPrice());
        channel.setCacheReadPrice(request.getCacheReadPrice());
        channel.setCacheWritePrice(request.getCacheWritePrice());
        channel.setInputAudioPrice(request.getInputAudioPrice());
        channel.setOutputAudioPrice(request.getOutputAudioPrice());
        channel.setQuotaLimit(request.getQuotaLimit());
        channel.setState(ChannelState.ACTIVE);

        // 设置 Provider 名称
        providerGateway.findById(request.getProviderId())
            .ifPresent(p -> channel.setProviderName(p.getName()));

        Channel saved = channelGateway.save(channel);
        log.info("Created channel: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + id));

        if (!channel.getName().equals(request.getName())) {
            if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
                throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
            }
        }

        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getProductType()));
        channel.setEndpoints(toJsonString(request.getEndpoints()));
        channel.setInputPrice(request.getInputPrice());
        channel.setOutputPrice(request.getOutputPrice());
        channel.setReasoningPrice(request.getReasoningPrice());
        channel.setCacheReadPrice(request.getCacheReadPrice());
        channel.setCacheWritePrice(request.getCacheWritePrice());
        channel.setInputAudioPrice(request.getInputAudioPrice());
        channel.setOutputAudioPrice(request.getOutputAudioPrice());
        channel.setQuotaLimit(request.getQuotaLimit());

        Channel saved = channelGateway.save(channel);
        log.info("Updated channel: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public ProductResponse getById(Long id) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + id));
        return toResponse(channel);
    }

    @Override
    public List<ProductResponse> getByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<ProductResponse> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode) {
        return channelGateway.findByProviderIdAndBillingMode(providerId, billingMode).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        channelGateway.deleteById(id);
        log.info("Deleted channel: id={}", id);
    }

    @SuppressWarnings("unchecked")
    private ProductResponse toResponse(Channel channel) {
        ProductResponse response = new ProductResponse();
        response.setId(channel.getId());
        response.setProviderId(channel.getProviderId());
        response.setProviderName(channel.getProviderName());
        response.setName(channel.getName());
        response.setProductType(channel.getBillingMode().getCode());
        response.setEndpoints(fromJsonString(channel.getEndpoints()));
        response.setInputPrice(channel.getInputPrice());
        response.setOutputPrice(channel.getOutputPrice());
        response.setReasoningPrice(channel.getReasoningPrice());
        response.setCacheReadPrice(channel.getCacheReadPrice());
        response.setCacheWritePrice(channel.getCacheWritePrice());
        response.setInputAudioPrice(channel.getInputAudioPrice());
        response.setOutputAudioPrice(channel.getOutputAudioPrice());
        response.setQuotaLimit(channel.getQuotaLimit());
        response.setState(channel.getState().getCode());
        return response;
    }

    /** 将 Map 序列化为 JSON 字符串 */
    private String toJsonString(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize endpoints map", e);
            return null;
        }
    }

    /** 将 JSON 字符串反序列化为 Map */
    @SuppressWarnings("unchecked")
    private Map<String, String> fromJsonString(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize endpoints json", e);
            return null;
        }
    }
}