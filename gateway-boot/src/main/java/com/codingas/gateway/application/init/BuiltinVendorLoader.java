/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.application.init;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.channel.ChannelEndpointGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.model.ModelInstanceGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内建厂商数据加载器
 *
 * <p>从 classpath:data/builtin/vendors/*.json 加载供应商、模型、渠道、端点和模型实例数据。</p>
 */
@Slf4j
@Component
public class BuiltinVendorLoader implements DataLoader {

    private static final String VENDORS_PATTERN = "data/builtin/vendors/*.json";

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ModelInstanceGateway modelInstanceGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;

    public BuiltinVendorLoader(ProviderGateway providerGateway,
                               ModelGateway modelGateway,
                               ModelInstanceGateway modelInstanceGateway,
                               ChannelGateway channelGateway,
                               ChannelEndpointGateway channelEndpointGateway) {
        this.providerGateway = providerGateway;
        this.modelGateway = modelGateway;
        this.modelInstanceGateway = modelInstanceGateway;
        this.channelGateway = channelGateway;
        this.channelEndpointGateway = channelEndpointGateway;
    }

    // ========== JSON DTO records ==========

    private record VendorData(
            String code, String name, String websiteUrl, String apiDocUrl, String description,
            List<ModelData> models, List<ChannelData> channels
    ) {}

    private record ModelData(String modelName, String displayName, int contextWindow) {}

    private record ChannelData(
            String key, String name, String billingMode,
            List<EndpointData> endpoints, List<ModelInstanceData> modelInstances
    ) {}

    private record EndpointData(String protocol, String url) {}

    private record ModelInstanceData(String modelName, String upstreamModelName, int priority, int weight) {}

    // ========== DataLoader ==========

    @Override
    public InitPhase getPhase() {
        return InitPhase.BUILTIN_VENDOR;
    }

    @Override
    public void load(DataLoadContext context) {
        log.info("加载内建厂商数据...");
        Map<String, Channel> channelMap = new HashMap<>();

        List<VendorData> vendors = JsonResourceReader.readListFromPattern(VENDORS_PATTERN, new TypeReference<>() {});
        if (vendors.isEmpty()) {
            throw new IllegalStateException("无法加载内建厂商数据，请检查 " + VENDORS_PATTERN);
        }

        for (VendorData vendor : vendors) {
            loadVendor(vendor, channelMap);
        }

        log.info("  共加载 {} 个厂商, {} 个渠道", providerGateway.count(), channelMap.size());
        context.set(DataLoadContext.ChannelIndex.class, new DataLoadContext.ChannelIndex(channelMap));
    }

    // ========== Internal methods ==========

    private void loadVendor(VendorData vendor, Map<String, Channel> channelMap) {
        Provider provider = getOrCreateProvider(vendor);

        if (vendor.models() != null) {
            for (ModelData model : vendor.models()) {
                ensureModel(model);
            }
        }

        if (vendor.channels() != null) {
            for (ChannelData channel : vendor.channels()) {
                Channel saved = createChannel(provider.getId(), channel);
                channelMap.put(channel.key(), saved);

                if (channel.endpoints() != null) {
                    for (EndpointData ep : channel.endpoints()) {
                        createEndpoint(saved.getId(), ep);
                    }
                }

                if (channel.modelInstances() != null) {
                    for (ModelInstanceData mi : channel.modelInstances()) {
                        createModelInstance(saved.getId(), mi);
                    }
                }
            }
        }
    }

    private Provider getOrCreateProvider(VendorData vendor) {
        return providerGateway.findByCode(vendor.code()).orElseGet(() -> {
            Provider provider = new Provider();
            provider.setCode(vendor.code());
            provider.setName(vendor.name());
            provider.setWebsiteUrl(vendor.websiteUrl());
            provider.setApiDocUrl(vendor.apiDocUrl());
            provider.setDescription(vendor.description());
            return providerGateway.save(provider);
        });
    }

    private void ensureModel(ModelData model) {
        if (modelGateway.findByModelName(model.modelName()).isPresent()) {
            return;
        }
        Model m = new Model();
        m.setModelName(model.modelName());
        m.setDisplayName(model.displayName());
        m.setContextWindow(model.contextWindow());
        modelGateway.save(m);
    }

    private Channel createChannel(Long providerId, ChannelData data) {
        return channelGateway.findByProviderIdAndName(providerId, data.name())
                .orElseGet(() -> {
                    log.info("  创建渠道: {}", data.name());
                    Channel channel = new Channel();
                    channel.setProviderId(providerId);
                    channel.setName(data.name());
                    channel.setBillingMode(BillingMode.valueOf(data.billingMode()));
                    return channelGateway.save(channel);
                });
    }

    private void createEndpoint(Long channelId, EndpointData data) {
        Protocol protocol = Protocol.valueOf(data.protocol());
        if (channelEndpointGateway.findByChannelIdAndProtocol(channelId, protocol).isPresent()) {
            return;
        }
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(data.url());
        channelEndpointGateway.save(endpoint);
    }

    private void createModelInstance(Long channelId, ModelInstanceData data) {
        Optional<Model> modelOpt = modelGateway.findByModelName(data.modelName());
        if (modelOpt.isEmpty()) {
            log.warn("  模型 '{}' 不存在，跳过模型实例", data.modelName());
            return;
        }
        Long modelId = modelOpt.get().getId();
        if (modelInstanceGateway.existsByChannelIdAndModelId(channelId, modelId)) {
            return;
        }
        ModelInstance instance = new ModelInstance();
        instance.setChannelId(channelId);
        instance.setModelId(modelId);
        instance.setUpstreamModelName(data.upstreamModelName());
        instance.setPriority(data.priority());
        instance.setWeight(data.weight());
        modelInstanceGateway.save(instance);
    }
}
