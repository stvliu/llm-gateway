package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
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
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.TeamChannel;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.enums.TeamRole;
import com.codingas.gateway.domain.team.enums.TeamState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据初始化器
 *
 * <p>基础设施初始化（admin 内置用户）在所有环境下无条件执行。</p>
 * <p>演示数据初始化（渠道、团队、演示用户）受 {@code gateway.init.demo-data-enabled} 配置控制。</p>
 * <ul>
 *   <li>admin 内置用户（无条件创建）</li>
 *   <li>5个主流大模型供应商（后备逻辑）</li>
 *   <li>17个最新大模型（后备逻辑）</li>
 *   <li>10个接入点（每个供应商2个套餐）</li>
 *   <li>4个团队（default、dev、product、openclaw）</li>
 *   <li>10个测试用户（test1-test10）</li>
 *   <li>10个API密钥（按团队分配不同权限）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // ==================== 常量定义 ====================
    
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    
    // 团队名称常量
    private static final String TEAM_DEFAULT = "default";
    private static final String TEAM_DEV = "dev";
    private static final String TEAM_PRODUCT = "product";
    private static final String TEAM_OPENCLAW = "openclaw";

    // ==================== 依赖注入 ====================

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final UserApiKeyGateway userApiKeyGateway;
    private final UserGateway userGateway;
    private final TeamGateway teamGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;
    private final PasswordEncoder passwordEncoder;
    private final GatewayProperties gatewayProperties;

    @Override
    @Transactional
    public void run(String... args) {
        // Phase 1: 基础设施 — 确保 admin 内置用户存在（无条件执行）
        ensureAdminUser();

        // Phase 2: 演示开关检查
        if (!gatewayProperties.getInit().isDemoDataEnabled()) {
            log.info("演示数据初始化已禁用 (demo-data-enabled=false)");
            return;
        }

        // Phase 2 (cont): 幂等守卫 — 演示数据是否已初始化
        if (userGateway.findByUsername("test1").isPresent()) {
            log.info("演示数据已存在，跳过初始化");
            return;
        }

        log.info("Initializing demo data...");

        // Phase 3: 执行初始化
        // 后备：如果 BuiltinDataLoader 未执行，补充创建供应商和模型
        if (providerGateway.count() == 0) {
            log.info("BuiltinDataLoader 未加载供应商数据，执行后备初始化");
            initializeProviders();
            initializeModels();
        }

        Map<String, Channel> channels = initializeChannels();
        List<Team> teams = initializeTeams();
        initializeTeamChannelAssignments(teams, channels);
        List<User> users = initializeDemoUsers();
        initializeUserTeamAssignments(users, teams);
        initializeApiKeys(users);

        logInitializationSummary((int) providerGateway.count(), channels.size(), teams.size(), users.size());
    }

    /**
     * 确保 admin 内置用户存在
     *
     * <p>admin 是系统基础设施用户，生产环境和开发环境都需要。
     * 如果 admin 已存在则跳过，不存在则创建。</p>
     */
    private void ensureAdminUser() {
        if (userGateway.findByUsername("admin").isPresent()) {
            return;
        }
        User admin = createUser("admin", "admin@example.com", ADMIN_ROLE, true);
        log.info("Admin 内置用户已创建 (id={})", admin.getId());
    }

    /**
     * 初始化供应商
     * 
     * <p>包含5+1个主流大模型供应商，补充网站和文档URL信息</p>
     */
    private Map<String, Provider> initializeProviders() {
        log.info("Step 1: Initializing providers...");
        
        Map<String, Provider> providers = new HashMap<>();
        
        // OpenAI - 全球领先的 AI 提供商
        Provider openai = createProvider("openai", "OpenAI", 
            "https://openai.com", 
            "https://platform.openai.com/docs/api-reference",
            "全球领先的人工智能研究公司，提供 GPT 系列模型");
        providers.put("openai", openai);
        
        // Anthropic - Claude 系列模型开发商
        Provider anthropic = createProvider("anthropic", "Anthropic", 
            "https://www.anthropic.com", 
            "https://docs.anthropic.com/en/api/getting-started",
            "专注于安全 AI 的研究公司，Claude 系列模型开发商");
        providers.put("anthropic", anthropic);
        
        // DeepSeek - 国内优秀的 AI 模型提供商
        Provider deepseek = createProvider("deepseek", "DeepSeek", 
            "https://www.deepseek.com", 
            "https://api-docs.deepseek.com",
            "深度求索，专注于通用人工智能的科技公司");
        providers.put("deepseek", deepseek);
        
        // 通义千问 - 阿里巴巴的 AI 模型
        Provider qwen = createProvider("qwen", "通义千问", 
            "https://tongyi.aliyun.com/qianwen", 
            "https://help.aliyun.com/zh/model-studio/developer-reference",
            "阿里云推出的大规模语言模型系列");
        providers.put("qwen", qwen);
        
        // 智谱AI - 清华系的 GLM 模型提供商
        Provider zhipu = createProvider("zhipu", "智谱AI", 
            "https://www.zhipuai.cn", 
            "https://open.bigmodel.cn/dev/api",
            "清华大学系 AI 公司，GLM 系列模型开发商");
        providers.put("zhipu", zhipu);
        
        // 火山方舟 - 字节跳动的 AI 平台
        Provider volcengine = createProvider("volcengine", "火山方舟", 
            "https://www.volcengine.com/product/ark", 
            "https://www.volcengine.com/docs/82379/1263482",
            "字节跳动推出的大模型服务平台，支持多种开源和自研模型");
        providers.put("volcengine", volcengine);
        
        log.info("  Created {} providers with website and documentation URLs", providers.size());
        return providers;
    }

    /**
     * 初始化模型
     * 
     * <p>包含17+3个最新大模型，覆盖各供应商的旗舰和主流型号</p>
     */
    private void initializeModels() {
        log.info("Step 2: Initializing models...");
        
        // OpenAI 模型（5个）
        createModel("gpt-5.5", "GPT-5.5", 270000);
        createModel("gpt-5.4", "GPT-5.4", 270000);
        createModel("gpt-5.4-mini", "GPT-5.4 Mini", 270000);
        createModel("gpt-4o", "GPT-4o", 128000);
        createModel("gpt-4o-mini", "GPT-4o Mini", 128000);
        
        // Anthropic 模型（3个）
        createModel("claude-sonnet-4-20250514", "Claude Sonnet 4", 200000);
        createModel("claude-3-7-sonnet-20250219", "Claude 3.7 Sonnet", 200000);
        createModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", 200000);
        
        // DeepSeek 模型（3个）
        createModel("deepseek-v4-pro", "DeepSeek-V4-Pro", 64000);
        createModel("deepseek-v4-flash", "DeepSeek-V4-flash", 64000);
        createModel("deepseek-r1", "DeepSeek-R1", 64000);
        
        // 通义千问模型（3个）
        createModel("qwen-max", "Qwen-Max", 32000);
        createModel("qwen-plus", "Qwen-Plus", 131072);
        createModel("qwen-turbo", "Qwen-Turbo", 1000000);
        
        // 智谱AI模型（3个）
        createModel("glm-5.1", "GLM-5.1", 128000);
        createModel("glm-4-plus", "GLM-4-Plus", 128000);
        createModel("glm-4-air", "GLM-4-Air", 128000);
        
        // 火山方舟模型（3个）
        createModel("doubao-pro-32k", "Doubao Pro 32K", 32000);
        createModel("doubao-lite-128k", "Doubao Lite 128K", 128000);
        createModel("skylark2-pro-4k", "Skylark2 Pro 4K", 4000);
        
        log.info("  Created 20 models across 6 providers");
    }

    /**
     * 初始化渠道（接入点）
     * 
     * <p>根据各供应商的实际计费模式配置，共12个接入点：</p>
     * <ul>
     *   <li><b>OpenAI</b>: 纯按量计费（PAY_AS_YOU_GO），无订阅制</li>
     *   <li><b>Anthropic</b>: 订阅制（SUBSCRIPTION），企业用混合计费（HYBRID）</li>
     *   <li><b>DeepSeek</b>: 按量计费 + 资源包</li>
     *   <li><b>通义千问</b>: 按量计费 + 资源包</li>
     *   <li><b>智谱AI</b>: 按量计费 + 分层订阅</li>
     *   <li><b>火山方舟</b>: 按量计费 + 预付费套餐</li>
     * </ul>
     */
    private Map<String, Channel> initializeChannels() {
        log.info("Step 2: Initializing channels...");
        
        Map<String, Channel> channels = new HashMap<>();
        
        // 获取已初始化的供应商
        Map<String, Provider> providers = new HashMap<>();
        providerGateway.findAll().forEach(p -> providers.put(p.getCode(), p));
        
        // ==================== OpenAI 渠道 ====================
        // OpenAI 采用纯按量计费模式，无固定订阅费
        channels.put("openai-default", createChannel(providers.get("openai").getId(), 
            "OpenAI 标准版", BillingMode.PAY_AS_YOU_GO));
        channels.put("openai-enterprise", createChannel(providers.get("openai").getId(), 
            "OpenAI 企业版", BillingMode.PAY_AS_YOU_GO));
        
        // ==================== Anthropic 渠道 ====================
        // Anthropic 个人/团队采用分层订阅，企业采用混合计费
        channels.put("anthropic-pro", createChannel(providers.get("anthropic").getId(), 
            "Anthropic Pro ($20/月)", BillingMode.SUBSCRIPTION));
        channels.put("anthropic-enterprise", createChannel(providers.get("anthropic").getId(),
            "Anthropic Enterprise ($20/用户+按量)", BillingMode.HYBRID));
        
        // ==================== DeepSeek 渠道 ====================
        // DeepSeek 支持按量计费和预付费资源包
        channels.put("deepseek-default", createChannel(providers.get("deepseek").getId(), 
            "DeepSeek 按量计费", BillingMode.PAY_AS_YOU_GO));
        channels.put("deepseek-package", createChannel(providers.get("deepseek").getId(), 
            "DeepSeek 资源包", BillingMode.PREPAID_PACKAGE));
        
        // ==================== 通义千问渠道 ====================
        // 通义千问支持按量计费和预付费资源包
        channels.put("qwen-default", createChannel(providers.get("qwen").getId(), 
            "通义千问按量计费", BillingMode.PAY_AS_YOU_GO));
        channels.put("qwen-package", createChannel(providers.get("qwen").getId(), 
            "通义千问资源包", BillingMode.PREPAID_PACKAGE));
        
        // ==================== 智谱AI渠道 ====================
        // 智谱AI支持按量计费和分层订阅
        channels.put("zhipu-default", createChannel(providers.get("zhipu").getId(), 
            "智谱AI按量计费", BillingMode.PAY_AS_YOU_GO));
        channels.put("zhipu-subscription", createChannel(providers.get("zhipu").getId(), 
            "智谱AI专业订阅", BillingMode.SUBSCRIPTION));
        
        // ==================== 火山方舟渠道 ====================
        // 火山方舟支持按量计费和预付费套餐
        channels.put("volcengine-default", createChannel(providers.get("volcengine").getId(), 
            "火山方舟按量计费", BillingMode.PAY_AS_YOU_GO));
        channels.put("volcengine-package", createChannel(providers.get("volcengine").getId(), 
            "火山方舟预付费套餐", BillingMode.PREPAID_PACKAGE));
        
        // 创建端点和凭证
        initializeChannelEndpointsAndCredentials(channels);
        
        log.info("  Created {} channels with realistic billing modes", channels.size());
        return channels;
    }

    /**
     * 初始化渠道端点和凭证
     * 
     * <p>为每个渠道配置协议、Base URL 和 API Key</p>
     */
    private void initializeChannelEndpointsAndCredentials(Map<String, Channel> channels) {
        log.info("Step 3: Initializing channel endpoints and credentials...");
        
        // ==================== OpenAI 端点 ====================
        createEndpoint(channels.get("openai-default").getId(), Protocol.OPENAI, "https://api.openai.com/v1");
        createEndpoint(channels.get("openai-enterprise").getId(), Protocol.OPENAI, "https://api.openai.com/v1");
        createChannelCredential(channels.get("openai-default").getId(), "sk-openai-default-key-001");
        createChannelCredential(channels.get("openai-enterprise").getId(), "sk-openai-enterprise-key-001");
        
        // ==================== Anthropic 端点 ====================
        createEndpoint(channels.get("anthropic-pro").getId(), Protocol.ANTHROPIC, "https://api.anthropic.com/v1");
        createEndpoint(channels.get("anthropic-enterprise").getId(), Protocol.ANTHROPIC, "https://api.anthropic.com/v1");
        createChannelCredential(channels.get("anthropic-pro").getId(), "sk-ant-pro-key-001");
        createChannelCredential(channels.get("anthropic-enterprise").getId(), "sk-ant-enterprise-key-001");
        
        // ==================== DeepSeek 端点 ====================
        createEndpoint(channels.get("deepseek-default").getId(), Protocol.OPENAI, "https://api.deepseek.com/v1");
        createEndpoint(channels.get("deepseek-package").getId(), Protocol.OPENAI, "https://api.deepseek.com/v1");
        createChannelCredential(channels.get("deepseek-default").getId(), "sk-deepseek-default-key-001");
        createChannelCredential(channels.get("deepseek-package").getId(), "sk-deepseek-package-key-001");
        
        // ==================== 通义千问端点 ====================
        createEndpoint(channels.get("qwen-default").getId(), Protocol.OPENAI, 
            "https://dashscope.aliyuncs.com/compatible-mode/v1");
        createEndpoint(channels.get("qwen-package").getId(), Protocol.OPENAI, 
            "https://dashscope.aliyuncs.com/compatible-mode/v1");
        createChannelCredential(channels.get("qwen-default").getId(), "sk-qwen-default-key-001");
        createChannelCredential(channels.get("qwen-package").getId(), "sk-qwen-package-key-001");
        
        // ==================== 智谱AI端点 ====================
        createEndpoint(channels.get("zhipu-default").getId(), Protocol.OPENAI, 
            "https://open.bigmodel.cn/api/paas/v4");
        createEndpoint(channels.get("zhipu-subscription").getId(), Protocol.OPENAI, 
            "https://open.bigmodel.cn/api/paas/v4");
        createChannelCredential(channels.get("zhipu-default").getId(), "sk-zhipu-default-key-001");
        createChannelCredential(channels.get("zhipu-subscription").getId(), "sk-zhipu-subscription-key-001");
        
        // ==================== 火山方舟端点 ====================
        // 火山方舟使用 OpenAI 兼容协议
        createEndpoint(channels.get("volcengine-default").getId(), Protocol.OPENAI, 
            "https://ark.cn-beijing.volces.com/api/v3");
        createEndpoint(channels.get("volcengine-package").getId(), Protocol.OPENAI, 
            "https://ark.cn-beijing.volces.com/api/v3");
        createChannelCredential(channels.get("volcengine-default").getId(), "ak-volcengine-default-key-001");
        createChannelCredential(channels.get("volcengine-package").getId(), "ak-volcengine-package-key-001");
        
        log.info("  Created 12 endpoints and 12 credentials for all channels");
    }

    /**
     * 初始化团队
     */
    private List<Team> initializeTeams() {
        log.info("Step 4: Initializing teams...");
        
        List<Team> teams = new ArrayList<>();
        teams.add(createTeam(TEAM_DEFAULT, "默认团队"));
        teams.add(createTeam(TEAM_DEV, "开发团队"));
        teams.add(createTeam(TEAM_PRODUCT, "产品团队"));
        teams.add(createTeam(TEAM_OPENCLAW, "OpenClaw 团队"));
        
        log.info("  Created {} teams", teams.size());
        return teams;
    }

    /**
     * 初始化团队-渠道关联
     * 
     * <p>为每个团队配置可访问的渠道集合，作为权限基线。</p>
     * <ul>
     *   <li><b>default</b>: OpenAI + Anthropic Pro（2个渠道）</li>
     *   <li><b>dev</b>: 全部12个渠道</li>
     *   <li><b>product</b>: 企业级/付费渠道（6个）</li>
     *   <li><b>OpenClaw</b>: DeepSeek + 智谱AI + 火山方舟（6个）</li>
     * </ul>
     */
    private void initializeTeamChannelAssignments(List<Team> teams, Map<String, Channel> channels) {
        log.info("Step 5: Initializing team-channel assignments...");
        
        // default 团队：只能访问 OpenAI 和 Anthropic Pro
        List<Long> defaultChannels = List.of(
            channels.get("openai-default").getId(),
            channels.get("anthropic-pro").getId()
        );
        assignChannelsToTeam(teams.get(0).getId(), defaultChannels);
        
        // dev 团队：可以访问所有12个渠道
        List<Long> allChannels = List.of(
            channels.get("openai-default").getId(), channels.get("openai-enterprise").getId(),
            channels.get("anthropic-pro").getId(), channels.get("anthropic-enterprise").getId(),
            channels.get("deepseek-default").getId(), channels.get("deepseek-package").getId(),
            channels.get("qwen-default").getId(), channels.get("qwen-package").getId(),
            channels.get("zhipu-default").getId(), channels.get("zhipu-subscription").getId(),
            channels.get("volcengine-default").getId(), channels.get("volcengine-package").getId()
        );
        assignChannelsToTeam(teams.get(1).getId(), allChannels);
        
        // product 团队：只能访问生产级渠道（Premium/Enterprise/Package/Subscription）
        List<Long> premiumChannels = List.of(
            channels.get("openai-enterprise").getId(),
            channels.get("anthropic-enterprise").getId(),
            channels.get("deepseek-package").getId(),
            channels.get("qwen-package").getId(),
            channels.get("zhipu-subscription").getId(),
            channels.get("volcengine-package").getId()
        );
        assignChannelsToTeam(teams.get(2).getId(), premiumChannels);
        
        // OpenClaw 团队：只能访问 DeepSeek、智谱AI 和火山方舟
        List<Long> openclawChannels = List.of(
            channels.get("deepseek-default").getId(), channels.get("deepseek-package").getId(),
            channels.get("zhipu-default").getId(), channels.get("zhipu-subscription").getId(),
            channels.get("volcengine-default").getId(), channels.get("volcengine-package").getId()
        );
        assignChannelsToTeam(teams.get(3).getId(), openclawChannels);
        
        log.info("  Assigned channels to {} teams", teams.size());
    }

    /**
     * 为团队分配渠道访问权限
     */
    private void assignChannelsToTeam(Long teamId, List<Long> channelIds) {
        for (Long channelId : channelIds) {
            TeamChannel teamChannel = new TeamChannel(teamId, channelId);
            teamChannelGateway.save(teamChannel);
        }
    }

    /**
     * 初始化演示用户
     *
     * <p>创建 test1-test10 共 10 个测试用户，密码与用户名相同。</p>
     */
    private List<User> initializeDemoUsers() {
        log.info("Step 6: Initializing demo users...");

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String username = "test" + i;
            String email = username + "@example.com";
            users.add(createUser(username, email, USER_ROLE, false));
        }

        log.info("  Created {} demo users (test1-test10)", users.size());
        return users;
    }

    /**
     * 初始化用户-团队关联
     */
    private void initializeUserTeamAssignments(List<User> users, List<Team> teams) {
        log.info("Step 7: Initializing user-team assignments...");
        
        Map<String, Team> teamMap = Map.of(
            TEAM_DEFAULT, teams.get(0),
            TEAM_DEV, teams.get(1),
            TEAM_PRODUCT, teams.get(2),
            TEAM_OPENCLAW, teams.get(3)
        );
        
        // admin 用户：加入所有团队并设置为 OWNER
        User admin = getAdminUser();
        for (Team team : teams) {
            addUserToTeam(admin.getId(), team.getId(), TeamRole.OWNER);
        }

        // default 团队: test1, test2
        addUserToTeam(users.get(0).getId(), teamMap.get(TEAM_DEFAULT).getId(), TeamRole.MEMBER);
        addUserToTeam(users.get(1).getId(), teamMap.get(TEAM_DEFAULT).getId(), TeamRole.MEMBER);

        // dev 团队: test3, test4(管理员), test5
        addUserToTeam(users.get(2).getId(), teamMap.get(TEAM_DEV).getId(), TeamRole.MEMBER);
        addUserToTeam(users.get(3).getId(), teamMap.get(TEAM_DEV).getId(), TeamRole.ADMIN);
        addUserToTeam(users.get(4).getId(), teamMap.get(TEAM_DEV).getId(), TeamRole.MEMBER);

        // product 团队: test6, test7(管理员)
        addUserToTeam(users.get(5).getId(), teamMap.get(TEAM_PRODUCT).getId(), TeamRole.MEMBER);
        addUserToTeam(users.get(6).getId(), teamMap.get(TEAM_PRODUCT).getId(), TeamRole.ADMIN);

        // OpenClaw 团队: test8, test9, test10(所有者)
        addUserToTeam(users.get(7).getId(), teamMap.get(TEAM_OPENCLAW).getId(), TeamRole.MEMBER);
        addUserToTeam(users.get(8).getId(), teamMap.get(TEAM_OPENCLAW).getId(), TeamRole.MEMBER);
        addUserToTeam(users.get(9).getId(), teamMap.get(TEAM_OPENCLAW).getId(), TeamRole.OWNER);
        
        log.info("  Assigned users to teams");
    }

    /**
     * 初始化API密钥
     * 
     * <p>API Key 通过用户所属团队继承渠道访问权限，不再直接关联渠道。</p>
     * <p>不同团队的权限由 Team ↔ Channel 关系控制：</p>
     * <ul>
     *   <li><b>admin</b>: 所有12个渠道（通过加入所有团队）</li>
     *   <li><b>default</b>: OpenAI + Anthropic Pro（2个渠道）</li>
     *   <li><b>dev</b>: 全部12个渠道</li>
     *   <li><b>product</b>: 企业级/付费渠道（6个）</li>
     *   <li><b>OpenClaw</b>: DeepSeek + 智谱AI + 火山方舟（6个）</li>
     * </ul>
     */
    private void initializeApiKeys(List<User> users) {
        log.info("Step 8: Initializing API keys...");

        // ==================== admin 管理员 ====================
        createUserApiKey(getAdminUser().getId(), "admin-master-key");

        // ==================== default 团队 ====================
        createUserApiKey(users.get(0).getId(), "default-team-key-1");
        createUserApiKey(users.get(1).getId(), "default-team-key-2");

        // ==================== dev 团队 ====================
        createUserApiKey(users.get(2).getId(), "dev-team-key-1");
        createUserApiKey(users.get(3).getId(), "dev-team-key-2");
        createUserApiKey(users.get(4).getId(), "dev-team-key-3");

        // ==================== product 团队 ====================
        createUserApiKey(users.get(5).getId(), "product-team-key-1");
        createUserApiKey(users.get(6).getId(), "product-team-key-2");

        // ==================== OpenClaw 团队 ====================
        createUserApiKey(users.get(7).getId(), "openclaw-team-key-1");
        createUserApiKey(users.get(8).getId(), "openclaw-team-key-2");
        createUserApiKey(users.get(9).getId(), "openclaw-team-key-3");

        log.info("  Created 11 API keys (1 admin + 10 team keys, channel permissions inherited from teams)");
    }

    /**
     * 记录初始化摘要
     */
    private void logInitializationSummary(int providerCount, int channelCount, 
                                          int teamCount, int userCount) {
        log.info("========================================");
        log.info("Development data initialized successfully!");
        log.info("========================================");
        log.info("  Providers: {} (OpenAI, Anthropic, DeepSeek, 通义千问, 智谱AI, 火山方舟)", providerCount);
        log.info("  Models: 20");
        log.info("  Channels: {}", channelCount);
        log.info("  Teams: {} (default, dev, product, openclaw)", teamCount);
        log.info("  Demo users: {} (test1-test10)", userCount);
        log.info("  UserApiKeys: 11 (1 admin + 10 team keys)");
        log.info("========================================");
        log.info("Test accounts:");
        log.info("  Admin (built-in) - Username: admin, Password: admin, Role: ADMIN");
        log.info("  Demo users - Username: test1 ~ test10, Password: same as username, Role: USER");
        log.info("========================================");
        log.info("Permission model (3-layer inheritance):");
        log.info("  Team ↔ Channel (M:N) - Define team's accessible channels");
        log.info("  User → Team (N:1) - User belongs to one team");
        log.info("  UserApiKey → User (N:1) - API Key inherits team's permissions");
        log.info("========================================");
        log.info("Channel access by team:");
        log.info("  admin: All 12 channels (via all teams as OWNER)");
        log.info("  default: OpenAI + Anthropic Pro (2 channels)");
        log.info("  dev: All 12 channels (full access)");
        log.info("  product: Premium/Enterprise channels (6 channels)");
        log.info("  openclaw: DeepSeek + Zhipu + Volcengine (6 channels)");
        log.info("========================================");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取 admin 用户
     *
     * <p>admin 由 ensureAdminUser() 确保存在，此方法用于在初始化流程中查找 admin 用户。</p>
     */
    private User getAdminUser() {
        return userGateway.findByUsername("admin")
            .orElseThrow(() -> new IllegalStateException("Admin user not found - ensureAdminUser() must be called first"));
    }

    private Provider createProvider(String code, String name, String websiteUrl, String apiDocUrl, String description) {
        Provider provider = new Provider();
        provider.setCode(code);
        provider.setName(name);
        provider.setWebsiteUrl(websiteUrl);
        provider.setApiDocUrl(apiDocUrl);
        provider.setDescription(description);
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    private Channel createChannel(Long providerId, String name, BillingMode billingMode) {
        Channel channel = new Channel();
        channel.setProviderId(providerId);
        channel.setName(name);
        channel.setBillingMode(billingMode);
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
        channelEndpointGateway.save(endpoint);
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

    private Team createTeam(String name, String description) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setState(TeamState.ACTIVE);
        return teamGateway.save(team);
    }

    private User createUser(String username, String email, String role, boolean builtin) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(username)); // 密码与用户名相同
        user.setRole(role);
        user.setState(UserState.ACTIVE);
        user.setBuiltin(builtin);
        return userGateway.save(user);
    }

    private void addUserToTeam(Long userId, Long teamId, TeamRole role) {
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setRole(role);
        userTeamGateway.save(userTeam);
    }

    private void createUserApiKey(Long userId, String name) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(userId);
        userApiKey.setName(name);
        // UUID 靠前确保 keyPrefix（前10位）唯一
        String uuid8 = java.util.UUID.randomUUID().toString().substring(0, 8);
        userApiKey.setKeyPlain("sk-" + uuid8 + "-" + name.toLowerCase().replace(" ", "-"));
        userApiKeyGateway.save(userApiKey);
    }
}
