# Spring MVC + 虚拟线程 + OkHttp 迁移规划

> **文档版本**: v1.0
> **目标版本**: v1.0.0
> **创建日期**: 2026-04-29
> **状态**: 规划中

---

## 一、迁移目标

将项目从 **WebFlux + JPA** 迁移到 **Spring MVC + JPA + 虚拟线程 + OkHttp**

### 1.1 核心变更

| 维度 | 当前（WebFlux） | 迁移后（Spring MVC） |
|------|----------------|-------------------|
| HTTP 框架 | WebFlux | Spring MVC |
| HTTP 客户端 | WebClient | OkHttp |
| 并发模型 | EventLoop | 虚拟线程 |
| 流式响应 | Flux/ServerSentEvent | SseEmitter |
| Domain 接口 | Mono/Flux | 同步返回 |

### 1.2 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                   Tomcat (虚拟线程)                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Controller → SseEmitter                             │  │
│  └───────────────────────────────────────────────────────┘  │
│                        ↓                                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Domain: LLMProviderPort (技术无关)                   │  │
│  │          StreamCallback (回调接口)                     │  │
│  └───────────────────────────────────────────────────────┘  │
│                        ↓                                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Infrastructure                                      │  │
│  │    DynamicAdapterRegistry（数据库配置驱动）             │  │
│  │    OkHttpOpenAIAdapter / OkHttpAnthropicAdapter      │  │
│  │    JPA Repository                                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、关键技术说明

### 2.1 为什么用虚拟线程弥补 QPS

虚拟线程在阻塞时自动挂起，不占用 OS 线程：

```
传统 OS 线程: 1000 并发 = 1000 线程 → 内存爆炸
虚拟线程:    1000 并发 = 少量平台线程调度 → 内存极低
```

10k QPS 完全可达：
- 虚拟线程挂起时不占用 OS 线程
- JPA 调用在虚拟线程中执行，自动挂起/恢复
- 无需 EventLoop，完全同步编程模型

### 2.2 OkHttp + 虚拟线程

OkHttp 是同步 HTTP 客户端，在虚拟线程中执行时：

```java
// 虚拟线程中执行 - 阻塞时自动挂起，不占 OS 线程
try (Response response = httpClient.newCall(request).execute()) {
    // 处理响应
}
```

关键配置：
```yaml
llm:
  okhttp:
    connect-timeout: 30
    read-timeout: 120   # 流式响应需要较长超时
    write-timeout: 30
```

### 2.3 流式响应（SseEmitter）

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestBody ChatRequest request) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    
    // 在虚拟线程中执行流式调用
    llmProvider.chatStream(request, new StreamCallbackImpl(chunk -> {
        emitter.send(SseEmitter.event().name("message").data(chunk));
    }));
    
    return emitter;
}
```

### 2.4 Domain 层技术无关原则

Gateway 接口必须是技术无关的：

```java
// ✅ 正确 - 技术无关
public interface LLMProviderPort {
    LLMResponse chat(LLMRequest request);
    void chatStream(LLMRequest request, StreamCallback callback);
}

// ❌ 错误 - 技术泄露
public interface LLMProviderPort {
    Mono<LLMResponse> chat(LLMRequest request);  // WebFlux 类型
    Mono<Void> chatStream(LLMRequest request, StreamCallback callback);
}
```

StreamCallback 是跨层数据载体（回调语义），不是技术实现细节。

---

## 三、Provider 动态管理

### 3.1 架构

Provider 配置存储在数据库 `providers` 表，运行时动态加载：

```
Provider 表（数据库）
    │
    │ 运行时读取
    ▼
ProviderGateway → 获取所有 Provider 配置
    │
    ▼
动态创建 OkHttp 适配器实例
    │
    ▼
AdapterRegistry（内存缓存，配置变更时刷新）
```

### 3.2 刷新机制

| 触发条件 | 方式 |
|---------|------|
| 启动时 | `DynamicAdapterRegistry.init()` |
| 定时刷新 | `@Scheduled(fixedRate = 300000)` 每 5 分钟 |
| 配置变更 | `ProviderChangedEvent` 事件监听 |

---

## 四、文件变更清单

### 4.1 新建文件

| 文件 | 说明 |
|------|------|
| `OkHttpConfiguration.java` | OkHttp Bean 配置 |
| `OkHttpOpenAIAdapter.java` | OpenAI OkHttp 适配器 |
| `OkHttpAnthropicAdapter.java` | Anthropic OkHttp 适配器 |
| `DynamicAdapterFactory.java` | 动态适配器工厂 |
| `DynamicAdapterRegistry.java` | 动态适配器注册表 |
| `ProviderChangedEvent.java` | 配置变更事件 |
| `ChatStreamController.java` | 流式响应控制器 |

### 4.2 修改文件

| 文件 | 改动 |
|------|------|
| `LLMProviderPort.java` | 移除 Mono/Flux，返回同步类型 |
| `LLMProviderRegistryImpl.java` | 改为使用 DynamicAdapterRegistry |
| `application.yml` | 添加 provider 配置 |

### 4.3 移除依赖

| 依赖 | 原因 |
|------|------|
| `spring-boot-starter-webflux` | 迁移到 Spring MVC |

### 4.4 新增依赖

| 依赖 | 版本 | 原因 |
|------|------|------|
| `okhttp` | 4.12.0 | HTTP 客户端 |

---

## 五、迁移步骤

### Phase 1: 基础设施

1. 移除 `spring-boot-starter-webflux` 依赖
2. 添加 `okhttp:4.12.0` 依赖
3. 创建 `OkHttpConfiguration.java`
4. 配置虚拟线程（`application.yml`）

### Phase 2: Domain 接口改造

1. 修改 `LLMProviderPort.java`，移除 Mono/Flux
2. 确认 `StreamCallback` 接口不变
3. 确保 Domain 层无技术泄露

### Phase 3: 适配器实现

1. 创建 `OkHttpOpenAIAdapter.java`
2. 创建 `OkHttpAnthropicAdapter.java`
3. 创建 `DynamicAdapterFactory.java`
4. 创建 `DynamicAdapterRegistry.java`

### Phase 4: Controller

1. 创建 `ChatStreamController.java`
2. 配置 SSE 端点
3. 测试流式响应

### Phase 5: Provider 动态加载

1. 创建 `ProviderChangedEvent.java`
2. 修改 `ProviderServiceImpl` 发布事件
3. 集成 `DynamicAdapterRegistry`
4. 测试配置变更刷新

### Phase 6: 测试与验证

1. 单元测试
2. 集成测试
3. 流式响应测试
4. 性能测试

---

## 六、工作量估算

| Phase | 任务 | 预估工时 |
|-------|------|---------|
| 1 | 基础设施 | 0.5 天 |
| 2 | Domain 接口改造 | 0.5 天 |
| 3 | 适配器实现 | 1-2 天 |
| 4 | Controller | 0.5 天 |
| 5 | Provider 动态加载 | 1 天 |
| 6 | 测试与验证 | 1-2 天 |
| **总计** | | **4.5-7.5 天** |

---

## 七、风险与注意事项

### 7.1 虚拟线程陷阱

| 风险 | 缓解措施 |
|------|---------|
| synchronized 块会 pin 住平台线程 | 使用 `ReentrantLock` 替代 |
| 本地方法调用会 pin 住 | 避免或使用异步 JNI |
| ThreadLocal 每个 VT 独立实例 | 注意内存泄漏 |

### 7.2 SseEmitter 注意

| 问题 | 解决方案 |
|------|---------|
| 默认超时 30 秒 | 设置 `new SseEmitter(0L)` |
| 客户端断开检测延迟 | 设置心跳或超时检测 |
| 代理服务器缓冲 | 设置 `X-Accel-Buffering: no` |
| 内存泄漏 | `onCompletion` 回调清理 |

### 7.3 Provider 配置

- API Key 通过环境变量注入
- 配置变更后需要刷新适配器缓存
- 定时任务确保配置同步

---

## 八、验证标准

- [ ] 流式响应正常工作（SSE）
- [ ] 10k QPS 可达
- [ ] Provider 配置从数据库动态加载
- [ ] 配置变更后自动刷新
- [ ] Domain 层无 Mono/Flux 引用
- [ ] 单元测试覆盖率 ≥80%
