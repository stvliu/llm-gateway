# Tasks: llm-provider-simulator

## 第一阶段：测试工具包

- [ ] T1: 创建 `ResponseTemplates` 响应模板工厂
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ResponseTemplates.java`
  - 内容：OpenAI 非流式/流式/错误模板 + Anthropic 非流式/流式/错误模板

- [ ] T2: 创建 `ProviderSimulator` MockWebServer 封装
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/support/ProviderSimulator.java`
  - 内容：start/close、enqueueSuccess/enqueueError/enqueueStream、takeRequest

- [ ] T3: 创建 `OpenAIUpstreamClientTest` 测试类
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/OpenAIUpstreamClientTest.java`
  - 覆盖：非流式调用（请求验证+响应反序列化）、流式调用（SSE 解析+DONE 标记）、429/401/500 错误分类、超时、连通性测试

- [ ] T4: 创建 `AnthropicUpstreamClientTest` 测试类
  - 路径：`gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/upstream/AnthropicUpstreamClientTest.java`
  - 覆盖：非流式调用（请求验证+响应反序列化）、流式调用（SSE 解析+message_stop 标记）、429/401/500 错误分类、超时、连通性测试

- [ ] T5: 运行全部测试验证通过
  - 执行：`./mvnw test -pl gateway-boot -Dtest="*OpenAIUpstreamClientTest,*AnthropicUpstreamClientTest"`

## 第二阶段：独立运行模拟服务

- [ ] T6: 创建 `gateway-simulator` Maven 模块骨架
  - 路径：`gateway-simulator/pom.xml`、父 POM 更新
  - 依赖：spring-boot-starter-web

- [ ] T7: 实现模拟端点 Controller
  - 路径：`gateway-simulator/src/main/java/.../controller/SimulatorController.java`
  - 端点：POST `/v1/chat/completions`、POST `/v1/messages`（含流式支持）

- [ ] T8: 实现响应模板和服务层
  - 路径：`gateway-simulator/src/main/java/.../template/`、`.../service/`
  - 内容：SimulatorResponseTemplates（复用第一阶段模板逻辑）、SimulatorModeService（模式管理）

- [ ] T9: 实现管理 API Controller
  - 路径：`gateway-simulator/src/main/java/.../controller/SimulatorAdminController.java`
  - 端点：切换模式（正常/限流/故障）、查看请求记录

- [ ] T10: 创建 Spring Boot 启动类和配置
  - 路径：`gateway-simulator/src/main/java/.../LLMProviderSimulatorApplication.java`
  - 配置：`application.yml`（端口、默认模式、请求记录容量）

- [ ] T11: 编写 gateway-simulator 集成测试
  - 路径：`gateway-simulator/src/test/java/.../`
  - 覆盖：正常/限流/故障模式切换、流式端点、管理 API

- [ ] T12: 全量回归测试
  - 执行：`./mvnw clean test` 确保所有模块测试通过
