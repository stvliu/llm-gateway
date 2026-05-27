package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
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
    private final ModelGateway modelGateway;
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
        Provider openai = createProvider("openai", "OpenAI");
        Provider anthropic = createProvider("anthropic", "Anthropic");
        Provider deepseek = createProvider("deepseek", "DeepSeek");
        Provider volcengine = createProvider("volcengine", "Volcengine");

        // ===== 2. 创建 Model =====
        createModel("gpt-4o", "GPT-4o", 128000);
        createModel("gpt-4o-mini", "GPT-4o Mini", 128000);
        createModel("gpt-3.5-turbo", "GPT-3.5 Turbo", 16385);
        createModel("claude-sonnet-4-20250514", "Claude Sonnet 4", 200000);
        createModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", 200000);
        createModel(" doubao-seed-2-0-mini-260428", "Doubao-Seed-2.0-mini", 64000);
        createModel(" glm-5.1", "doubao-seed-2-0-mini-260428", 64000);
        createModel("deepseek-v4-pro", "DeepSeek Reasoner", 64000);
        createModel("deepseek-v4-flash", "DeepSeek-V4-flash", 64000);

        // ===== 3. 创建 Channel =====
        Channel openaiChannel = createChannel(openai.getId(), "OpenAI Standard");
        Channel anthropicChannel = createChannel(anthropic.getId(), "Anthropic Standard");
        Channel deepseekChannel = createChannel(deepseek.getId(), "DeepSeek Standard");

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

    private Provider createProvider(String code, String name) {
        Provider provider = new Provider();
        provider.setCode(code);
        provider.setName(name);
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    private Channel createChannel(Long providerId, String name) {
        Channel channel = new Channel();
        channel.setProviderId(providerId);
        channel.setName(name);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        return channelGateway.save(channel);
    }

    private void createModel(String modelName, String displayName, int contextWindow) {
        Model model = new Model();
        model.setModelName(modelName);
        model.setDisplayName(displayName);
        model.setContextWindow(contextWindow);
        model.setState(ModelState.ACTIVE);
        modelGateway.save(model);
    }

    private void createEndpoint(Long channelId, Protocol protocol, String url) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(url);
        endpoint.setState(ChannelEndpointState.ACTIVE);
        channelEndpointGateway.save(endpoint);
    }

    private void createChannelCredential(Long channelId, String plainApiKey) {
        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(channelId);
        credential.setApiKeyPlain(plainApiKey);
        credential.setApiKeyEncrypted("ENCRYPTED:" + plainApiKey);
        credential.setApiKeyPrefix(plainApiKey.substring(0, Math.min(8, plainApiKey.length())));
        credential.setName("default");
        credential.setState(CredentialState.ACTIVE);
        channelCredentialGateway.save(credential);
    }
}