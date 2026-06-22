# Brainstorm Summary — resilience-architecture (design 阶段)

> 恢复检查点。design 阶段逐个深化待定项,每轮澄清后增量更新。已确认内容为定论,待确认内容标注「待确认/候选」。

## 待深化点清单

1. P-r 数据迁移策略 ✅ 已确认
2. 无 Application 的 Key 兼容 ✅ 已确认
3. ApplicationChannel 与 ApplicationModel 关系 ✅ 已确认
4. 权限锚点切换的 ADMIN 语义 ✅ 已确认
5. L1 候选列表与权限边界 ✅ 已确认
6. 会话亲和存储 ✅ 已确认
7. Cluster 软分域→显式实体迁移 ✅ 已确认

## 已确认决策

### 1. P-r 数据迁移策略(方案 C:1:1 平移 + 后续细分)
- **映射规则**:1 Team → 1 默认 Application(code/name 继承 Team);TeamChannel → ApplicationChannel 1:1 平移,保证授权不丢不失真。
- **应用细分**(ClaudeCode/IPD)不在迁移脚本做,留待运维后续手动建应用、迁 Key 与渠道授权——应用归属是业务决策,不该让脚本猜。
- **迁移脚本设计**:可重跑、幂等;迁移后校验每个 Application 渠道集合 == 原 Team 渠道集合;Team 表暂留(验证通过后清理)。
- **Key 归属**:UserApiKey 经 UserTeam 找 teamId → 对应 Application。多 Team 用户的 Key 归属见待深化点 2。
- **理由**:迁移首要目标是授权不丢(1:1 最安全、可全自动);应用细分价值主要在 P2 画像,P-r 阶段应用粒度粗不影响 P-r 验证;与 proposal 非目标(本 change 不做应用级配额/看板)一致。

### 2. 无 Application 归属的 Key 兼容(方案 D:兜底应用)
- **迁移期**:归属不明的 Key(多 Team 用户、迁移遗漏)归 `migration-default` 应用,挂 default 画像,业务不中断。
- **运行期**:无 application_id 的 Key(直接 DB 建的、异常)软兜底归 `migration-default` + 告警日志,事后人工补 application_id。不硬失败(迁移期风险大)。
- **migration-default 渠道授权范围**:待深化点 4(ADMIN 语义)一起定——给全部活跃渠道(权限放大风险)还是空(不可用)还是等同原 ADMIN 兜底。
- **过渡产物**:migration-default 是迁移安全网,迁移完成后应清理(所有 Key 应有明确 application_id),非长期机制。
- **理由**:迁移首要不阻断业务;多 Team 用户是少数,不为其引入「主团队」过度设计;人工指定增加摩擦;1 Key 1 App 制造大量临时应用。兜底应用最简单且可后续细分。

### 3. ApplicationChannel 与 ApplicationModel 关系(方案 B:模型从属渠道,砍模型可见性)
- **决策**:只配 ApplicationChannel,模型可见性由「渠道上挂哪些 ModelInstance」隐式决定。**不建独立 ApplicationModel 实体**,模型可见性不再独立授权。
- **废弃现有机制**:现有「团队模型可见性」(`teamApi.listAllowedModels` + 前端 `ModelVisibilityModal`)废弃删除。模型可见性收敛到供给侧(渠道配置 ModelInstance)。
- **后果**:无法「授权渠道但限制模型」——应用要某模型就授权挂该模型的渠道,不想给就别授权或渠道别挂。同一渠道下的多应用共享该渠道全部模型。
- **过滤链**:单层 ApplicationChannel(应用授权哪些渠道)→ 渠道上的 ModelInstance 决定可用模型。
- **对 proposal 的影响**:移除 `ApplicationModel` 实体/表;`application-access-control` capability 范围收窄为仅 ApplicationChannel;前端移除 ModelVisibilityModal;tasks 1.3 删除、1.2/1.10 调整。
- **理由**:用户判断模型可见性独立配置是过度设计,供给侧管够了;简化为单层渠道过滤,语义更直接。

### 4. 权限锚点切换的 ADMIN 语义(方案 C:管理特权,数据面无跳过)
- **决策**:ADMIN 退回管理面特权——保留 Sa-Token `@SaCheckRole("ADMIN")` 在管理面 Controller(管应用/渠道/画像配置);**数据面 `PermissionRouter` 一律走 ApplicationChannel,无 ADMIN 跳过后门**。
- **管理员调试**:不再用「ADMIN Key 调任意渠道」,改为建专门「全渠道调试应用」授权全部渠道、挂宽松画像(可审计、可回收)。
- **migration-default 渠道范围**(解决深化点 2 遗留):兜底应用**不全局开放**,而是**继承迁移前该 Key 所属 Team 的渠道集**——既不阻断业务(原渠道还能用)又不放大权限。
- **对 proposal 的影响**:P-r 任务增加「移除 PermissionRouter 的 ADMIN 跳过分支」;迁移脚本为 migration-default 按原 Team 渠道集授权(而非全局)。
- **理由**:职责清晰(管理特权归管理面,数据面纯走应用授权);权限模型纯粹无后门;符合「应用为中心」定位;避免「权限锚点是应用却给人开特权」的矛盾。

### 5. L1 候选列表与权限边界(位置 A:路由产候选)
- **候选列表构成**:`findActiveByModelIdOrderByPriority(modelId)` ∩ `ApplicationChannel(当前 Key 的 applicationId)` → 按 (cluster_code, priority) 排序。
- **权限边界天然限定**:PermissionRouter(ApplicationChannel 过滤)先跑产出候选,ChannelFailoverInvoker 只在已过滤列表内转移,跨 Cluster 转移不越权(不会访问应用未授权渠道)。
- **实现位置**:RouterChain(PermissionRouter+HealthRouter+PriorityRouter)联合产出「应用可见+健康+按 cluster/priority 排序」候选列表;LoadBalanceRouter 从「选一个」改为返回排序列表(或退场由 ChannelFailoverInvoker 接管选择)。权限只算一次。
- **转移中熔断**:候选列表权限/优先级静态算好,熔断状态动态检查——ChannelFailoverInvoker 逐个尝试时实时查 `circuitBreakerManager.isAvailable(endpointId)`(P0 已统一 key),跳过已熔断试下一个。
- **理由**:权限只算一次转移快;权限边界在路由阶段一次性确定,转移回路职责清晰(只管在已授权候选间按 cluster/priority 试)。

### 6. 会话亲和存储(方案 A:Redis + 内存双实现)
- **选型**:SessionAffinityStore 接口 + RedisSessionAffinityStore(生产)/ InMemorySessionAffinityStore(开发)双实现,复用现有 TokenBucketRateLimiter 模式。
- **映射**:`session-affinity:{sessionId}:{modelId} → channelId`,TTL 30min 可配(`gateway.resilience.session-affinity.ttl`)。
- **亲和是优先非强制**:首选亲和渠道;亲和渠道熔断 → 让位于可用性,跳过试候选下一个,并**更新亲和映射到新渠道**;亲和渠道已不在 ApplicationChannel 授权内 → 丢弃亲和重新选。
- **标识缺失**:X-Session-Id 缺失 → 不亲和(安全降级)。
- **理由**:网关 10000 QPS 必多实例,会话亲和须跨实例一致 → 必分布式存储;项目已有 Redis 基础设施非新增依赖;复用限流器双实现模式架构一致。

### 7. Cluster 软分域→显式实体迁移(方案 C:合并 P2/P3,直接建实体)
- **决策**:P2 直接建 Cluster 实体(clusters 表 + Channel.cluster_id FK),**不经历 cluster_code 软字段阶段**。合并原 P2(画像)+ P3(Cluster)为一段。
- **路线图简化**:P-r/P0/P1/P2/P3 五段 → P-r/P0/P1/P2 四段(P2=画像+Cluster)。
- **理由**:P2 转移逻辑(按 cluster 排序、域内/跨域)已需 cluster 概念,软字段与实体访问方式不同,分期会改两遍转移逻辑(返工);Cluster 实体本身轻(clusters 表成本低),不值得为省而引入软字段过渡;软字段阶段无独立验证价值(只是实体劣化版)。
- **对 proposal 的影响**:移除 Channel.cluster_code 软字段,直接 Channel.cluster_id FK + clusters 表;P2/P3 合并;tasks 第 5 组并入第 4 组;路线图 5 段变 4 段。

---

## design 阶段深化完成

7 个待深化点全部确认。对 proposal/design/tasks 的累积影响:
1. **砍 ApplicationModel**(深化点 3):模型可见性由渠道 ModelInstance 决定,不独立授权;废弃现有团队模型可见性机制与前端 Modal。
2. **ADMIN 退管理面**(深化点 4):数据面 PermissionRouter 无跳过;migration-default 按原 Team 渠道集授权。
3. **合并 P2/P3**(深化点 7):直接建 Cluster 实体,路线图 5 段变 4 段。

待办:据此更新 design.md 与 tasks.md,运行 design guard 退出。
