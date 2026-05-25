package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 开发环境数据初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProviderGateway providerGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final UserApiKeyGateway userApiKeyGateway;

    @Override
    @Transactional
    public void run(String... args) {
        if (providerGateway.count() > 0) {
            log.info("Data already initialized, skipping...");
            return;
        }

        log.info("Initializing development data...");

        // ===== 1. 创建 Provider =====
        Provider openai = new Provider();
        openai.setName("OpenAI");
        openai.setState(ProviderState.ACTIVE);
        openai = providerGateway.save(openai);

        Provider anthropic = new Provider();
        anthropic.setName("Anthropic");
        anthropic.setState(ProviderState.ACTIVE);
        anthropic = providerGateway.save(anthropic);

        Provider deepseek = new Provider();
        deepseek.setName("DeepSeek");
        // TODO: baseUrl 已下沉到 ChannelEndpoint，初始化数据时应通过 ChannelEndpoint 设置
        deepseek.setState(ProviderState.ACTIVE);
        deepseek = providerGateway.save(deepseek);

        // ===== 2. 创建 ModelSpec =====
        createModelSpec(openai.getId(), "gpt-4o", "GPT-4o", 128000);
        createModelSpec(openai.getId(), "gpt-4o-mini", "GPT-4o Mini", 128000);
        createModelSpec(openai.getId(), "gpt-3.5-turbo", "GPT-3.5 Turbo", 16385);
        createModelSpec(anthropic.getId(), "claude-sonnet-4-20250514", "Claude Sonnet 4", 200000);
        createModelSpec(anthropic.getId(), "claude-3-5-haiku-20241022", "Claude 3.5 Haiku", 200000);
        createModelSpec(deepseek.getId(), "deepseek-chat", "DeepSeek Chat", 64000);
        createModelSpec(deepseek.getId(), "deepseek-reasoner", "DeepSeek Reasoner", 64000);

        // ===== 3. 创建 Channel =====
        Channel openaiChannel = new Channel();
        openaiChannel.setProviderId(openai.getId());
        openaiChannel.setName("OpenAI Standard");
        openaiChannel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        openaiChannel.setState(ChannelState.ACTIVE);
        openaiChannel = channelGateway.save(openaiChannel);

        Channel anthropicChannel = new Channel();
        anthropicChannel.setProviderId(anthropic.getId());
        anthropicChannel.setName("Anthropic Standard");
        anthropicChannel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        anthropicChannel.setState(ChannelState.ACTIVE);
        anthropicChannel = channelGateway.save(anthropicChannel);

        Channel deepseekChannel = new Channel();
        deepseekChannel.setProviderId(deepseek.getId());
        deepseekChannel.setName("DeepSeek Standard");
        deepseekChannel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        deepseekChannel.setState(ChannelState.ACTIVE);
        deepseekChannel = channelGateway.save(deepseekChannel);

        // ===== 4. 创建 ChannelEndpoint =====
        createEndpoint(openaiChannel.getId(), Protocol.OPENAI, "https://api.openai.com");
        createEndpoint(anthropicChannel.getId(), Protocol.ANTHROPIC, "https://api.anthropic.com");
        createEndpoint(deepseekChannel.getId(), Protocol.OPENAI, "https://api.deepseek.com");

        // ===== 5. 创建 ChannelCredential =====
        createChannelCredential(openaiChannel.getId(), "sk-openai-dev-key-001");
        createChannelCredential(anthropicChannel.getId(), "sk-ant-anthropic-dev-key-001");
        createChannelCredential(deepseekChannel.getId(), "sk-deepseek-dev-key-001");

        // ===== 6. 创建 UserApiKey =====
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setName("开发测试密钥");
        userApiKey.setUserId(1L);
        userApiKey.setChannelIds(List.of(openaiChannel.getId(), anthropicChannel.getId(), deepseekChannel.getId()));
        userApiKey.setModels(null); // null = 允许所有模型
        userApiKeyGateway.save(userApiKey);

        log.info("Development data initialized successfully!");
        log.info("  Providers: 3 (OpenAI, Anthropic, DeepSeek)");
        log.info("  Models: 7");
        log.info("  Channels: 3");
        log.info("  UserApiKeys: 1");
    }

    private void createModelSpec(Long providerId, String providerModelId, String displayName, int contextWindow) {
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setProviderModelId(providerModelId);
        modelSpec.setDisplayName(displayName);
        modelSpec.setContextWindow(contextWindow);
        modelSpec.setState(ModelSpecState.ACTIVE);
        modelSpecGateway.save(modelSpec);
    }

    private void createChannelCredential(Long channelId, String plainApiKey) {
        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(channelId);
        credential.setApiKeyPlain(plainApiKey);
        credential.setApiKeyPrefix(plainApiKey.substring(0, Math.min(8, plainApiKey.length())));
        credential.setName("default");
        credential.setState(CredentialState.ACTIVE);
        channelCredentialGateway.save(credential);
    }

    private void createEndpoint(Long channelId, Protocol protocol, String url) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(url);
        endpoint.setState(ChannelEndpointState.ACTIVE);
        channelEndpointGateway.save(endpoint);
    }
}