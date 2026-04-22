# Research: Provider Adapter Framework

## Decision: Java SPI + Adapter Pattern

**Chosen Approach**: Service Provider Interface (SPI) + Adapter Pattern

**Rationale**: Java SPI 是 JDK 内置的插件机制，通过 `META-INF/services` 自动发现实现，适合需要动态加载多种 Provider 的场景。适配器模式将不同 Provider 的差异封装在各自实现中，框架只需依赖抽象接口。

**Alternatives Considered**:
1. **Spring FactoryBean**: 需要依赖 Spring 特定机制，不够通用
2. **策略模式 + 简单工厂**: 需要手动注册，新增 Provider 需修改工厂代码，违反开闭原则
3. **反射 + 注解扫描**: 过于复杂，SPI 已足够满足需求

---

## Decision: Provider 接口方法设计

**Chosen Methods**:
```java
public interface LLMProviderAdapter {
    // 聊天补全（OpenAI 格式）
    ChatCompletionResult chatCompletion(ChatCompletionRequest request);
    
    // 消息 API（Anthropic 格式）
    MessagesResult messages(MessagesRequest request);
    
    // 向量嵌入
    EmbeddingResult embeddings(EmbeddingRequest request);
    
    // 获取 Provider 能力描述
    ProviderCapabilities getCapabilities();
    
    // 健康检查
    boolean isHealthy();
}
```

**Rationale**:
- `chatCompletion` vs `messages` 分离设计，避免在接口层做格式转换
- `getCapabilities()` 返回该 Provider 支持的功能列表（支持 streaming、function calling 等）
- 分离设计使得未来新增 Provider 只需实现接口，无需修改现有代码

**Alternatives Considered**:
1. **统一 `invoke()` 方法**: 参数使用泛型 Request/Response，但类型检查延迟到运行时
2. **多个具体方法**: 每个方法参数各异，但实现复杂度高

---

## Decision: Channel 多 Key 轮换策略

**Chosen Strategy**: 按优先级权重 + 失败自动切换

```
Key1 (priority=80) → 轮询 80% 请求
Key2 (priority=20) → 轮询 20% 请求

当 Key1 失败（超时/限流）:
  → 立即标记为 unhealthy
  → 切换到 Key2
  → 30秒后重试 Key1（健康检查）
```

**Rationale**: 优先级权重保证流量按配置分配，故障自动转移确保高可用。

**Alternatives Considered**:
1. **简单轮询**: 无法处理权重配置
2. **最小使用数策略**: 适合负载均衡场景，但不适用于 API Key 管理（Key 有限额）

---

## Decision: 热加载机制

**Chosen Approach**: Spring `@RefreshScope` + 配置变更事件

```java
@ConfigurationProperties
@RefreshScope
public class ProviderConfig {
    private String baseUrl;
    private String apiKey;  // 加密存储
    private int priority;
}
```

当 Provider 配置变更时：
1. 发布 `EnvironmentChangeEvent`
2. 所有 `@RefreshScope` Bean 重新创建
3. 进行中请求使用旧配置，完成后切换新配置

**Rationale**: Spring Boot 原生支持，无需引入额外框架。

---

## Decision: 模型列表预填充

**Approach**: Flyway 迁移脚本 + 初始化数据

```
db/migration/
├── V1__init_schema.sql           # 创建表结构
└── V2__seed_providers.sql        # 预填充 Provider + Model 数据
```

**50+ 主流模型预填充**:
- OpenAI: gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo, text-embedding-3-small, text-embedding-3-large
- Anthropic: claude-opus-4-5, claude-sonnet-4-6, claude-haiku-4-5
- Google: gemini-pro, gemini-pro-vision
- Azure OpenAI: gpt-4o-azure, gpt-4-turbo-azure
- 通义千问: qwen-turbo, qwen-plus, qwen-max
- 智谱: glm-4, glm-4-flash, glm-3-turbo
- 其他: Mistral, Cohere, Replicate 等

**Rationale**: 开箱即用，用户无需手动添加主流模型配置。