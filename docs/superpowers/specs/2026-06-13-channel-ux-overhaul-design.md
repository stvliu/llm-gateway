---
comet_change: channel-ux-overhaul
role: technical-design
canonical_spec: openspec
---

# Design Doc: Channel UX Overhaul (Phase 1)

## 概览

本设计文档承载 channel-ux-overhaul change 的深度技术决策。需求与验收 scenario 仍由 `openspec/changes/channel-ux-overhaul/specs/*/spec.md` 作为 canonical 来源；本文档聚焦"如何实现"。

## 1. 状态 SSOT 整合

### 决策

新建 `gateway-console/src/domain/channel/lifecycle.ts` 作为五状态生命周期的唯一来源，**完全替换无别名**。删除 `ChannelStateTag.STATE_CONFIG` 与 `stateTransitions.STATE_TRANSITION_LABELS`（仅保留 `stateTransitions.ts` 中可能仍被引用的纯类型/枚举导出）。

### 数据结构

```typescript
// gateway-console/src/domain/channel/lifecycle.ts

export type ChannelState =
  | 'PENDING'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'DEPRECATED'
  | 'RETIRED';

export type LifecycleVisualStyle = 'normal' | 'muted' | 'strikethrough';

export interface LifecycleMeta {
  /** i18n key，如 channel.state.active */
  label: string;
  /** Tooltip 主文案 i18n key */
  descriptionKey: string;
  /** 用于卡片左边框、健康点等十六进制色 */
  color: string;
  /** antd Tag 语义色，如 success / warning / error / default */
  tagColor: string;
  /** 是否参与流量分配 */
  isRoutable: boolean;
  /** 是否计费 */
  isBilling: boolean;
  /** 后续可转换至的状态（不含自身） */
  nextStates: readonly ChannelState[];
  /** 卡片视觉风格 */
  visualStyle: LifecycleVisualStyle;
}

export const CHANNEL_LIFECYCLE: Record<ChannelState, LifecycleMeta> = {
  PENDING:    { label: 'channel.state.pending',    descriptionKey: 'channel.state.pendingDesc',    color: '#d48806', tagColor: 'warning', isRoutable: false, isBilling: false, nextStates: ['ACTIVE'],                visualStyle: 'normal' },
  ACTIVE:     { label: 'channel.state.active',     descriptionKey: 'channel.state.activeDesc',     color: '#52c41a', tagColor: 'success', isRoutable: true,  isBilling: true,  nextStates: ['SUSPENDED', 'DEPRECATED'], visualStyle: 'normal' },
  SUSPENDED:  { label: 'channel.state.suspended',  descriptionKey: 'channel.state.suspendedDesc',  color: '#bfbfbf', tagColor: 'default', isRoutable: false, isBilling: false, nextStates: ['ACTIVE', 'DEPRECATED', 'RETIRED'], visualStyle: 'muted' },
  DEPRECATED: { label: 'channel.state.deprecated', descriptionKey: 'channel.state.deprecatedDesc', color: '#fa8c16', tagColor: 'warning', isRoutable: true,  isBilling: true,  nextStates: ['RETIRED'],               visualStyle: 'normal' },
  RETIRED:    { label: 'channel.state.retired',    descriptionKey: 'channel.state.retiredDesc',    color: '#ff4d4f', tagColor: 'error',   isRoutable: false, isBilling: false, nextStates: [],                         visualStyle: 'strikethrough' },
};

// Selector helpers — 纯函数便于 unit test
export const isRoutable = (s: ChannelState) => CHANNEL_LIFECYCLE[s].isRoutable;
export const isBilling = (s: ChannelState) => CHANNEL_LIFECYCLE[s].isBilling;
export const allowedTransitions = (s: ChannelState) => CHANNEL_LIFECYCLE[s].nextStates;
export const canTransitionTo = (from: ChannelState, to: ChannelState) =>
  CHANNEL_LIFECYCLE[from].nextStates.includes(to);

// Tooltip 文案构建 — 由调用方注入 i18n t 函数
export function buildStateTooltip(state: ChannelState, t: (key: string) => string): string {
  const meta = CHANNEL_LIFECYCLE[state];
  return [
    t(meta.descriptionKey),
    `${t('channel.state.tooltipRoutable')}: ${meta.isRoutable ? t('common.yes') : t('common.no')}`,
    `${t('channel.state.tooltipBilling')}: ${meta.isBilling ? t('common.yes') : t('common.no')}`,
    meta.nextStates.length > 0
      ? `${t('channel.state.tooltipNext')}: ${meta.nextStates.map(s => t(CHANNEL_LIFECYCLE[s].label)).join(' / ')}`
      : t('channel.state.tooltipTerminal'),
  ].join('\n');
}
```

### 改造步骤

1. 用 codegraph 列出 `STATE_CONFIG` / `STATE_TRANSITION_LABELS` 所有引用点
2. 新建 `lifecycle.ts` 与对应 i18n key（中英文）
3. 逐一替换引用：`STATE_CONFIG[state].color` → `CHANNEL_LIFECYCLE[state].color`
4. 删除旧文件中的无用导出，保留必须的纯类型/枚举（如 `ChannelState` union）
5. 单元测试覆盖 `CHANNEL_LIFECYCLE` 五条记录的字段不变性 + selector 函数

## 2. 反馈 hook 与脉冲动画

### useSavePulse

```typescript
// gateway-console/src/components/common/useSavePulse.ts

export type PulseState = 'idle' | 'success' | 'error';

export function useSavePulse() {
  const [state, setState] = useState<PulseState>('idle');
  const [errorMsg, setErrorMsg] = useState<string>();
  const ref = useRef<HTMLElement>(null);
  const timerRef = useRef<number>();

  const triggerSuccess = useCallback(() => {
    setState('success');
    setErrorMsg(undefined);
    window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => setState('idle'), 3000);
  }, []);

  const triggerError = useCallback((msg: string) => {
    setState('error');
    setErrorMsg(msg);
    window.clearTimeout(timerRef.current);
    // 错误态不自动清除，直到下次 trigger* 调用
  }, []);

  useEffect(() => () => window.clearTimeout(timerRef.current), []);

  const className =
    state === 'success' ? 'save-pulse-success' :
    state === 'error'   ? 'save-pulse-error'   : '';

  return { ref, state, errorMsg, className, triggerSuccess, triggerError };
}
```

### CSS

```css
/* gateway-console/src/components/common/SavePulse.css */

@keyframes save-pulse-success {
  0%   { background-color: rgba(82, 196, 26, 0); }
  20%  { background-color: rgba(82, 196, 26, 0.20); }
  100% { background-color: rgba(82, 196, 26, 0); }
}

.save-pulse-success { animation: save-pulse-success 800ms ease-out; }

.save-pulse-error {
  box-shadow: inset 0 0 0 1px var(--ant-color-error, #ff4d4f);
  transition: box-shadow 200ms ease-out;
}

@media (prefers-reduced-motion: reduce) {
  .save-pulse-success {
    animation: none;
    background-color: rgba(82, 196, 26, 0.10);
    transition: background-color 600ms ease-out;
  }
}

.save-tip-ok  { color: var(--ant-color-success); margin-left: 8px; }
.save-tip-err { color: var(--ant-color-error); margin-left: 8px; }
```

### 调用示例

```tsx
const pulse = useSavePulse();
const mutation = useMutation({
  mutationFn: (v: EndpointForm) => updateEndpoint(channelId, v),
  onMutate: async (v) => {                     // 乐观更新 + 备份
    await queryClient.cancelQueries({ queryKey });
    const prev = queryClient.getQueryData(queryKey);
    queryClient.setQueryData(queryKey, applyOptimistic(v));
    return { prev };
  },
  onError: (err, _v, ctx) => {
    queryClient.setQueryData(queryKey, ctx?.prev); // 回滚
    pulse.triggerError(extractMsg(err));
  },
  onSuccess: () => pulse.triggerSuccess(),
});

return (
  <li ref={pulse.ref} className={pulse.className}>
    <span>{endpoint.url}</span>
    {pulse.state === 'success' && <span className="save-tip-ok">✓ {t('save.success')}</span>}
    {pulse.state === 'error'   && <span className="save-tip-err">✗ {pulse.errorMsg}</span>}
  </li>
);
```

## 3. 测试矩阵 API

### 端点

```
POST /api/channels/{id}/health-check
Auth: ADMIN
Request:
  { "source": "CARD" | "DRAWER" | "PRECHECK" }
Response 200:
  {
    "channelId": number,
    "aggregateStatus": "HEALTHY" | "DEGRADED" | "FAILED" | "UNKNOWN",
    "startedAt": ISO-8601,
    "finishedAt": ISO-8601,
    "matrix": [
      {
        "credentialId": number,
        "keyMasked": string,         // 后端脱敏，如 "sk-***...wxyz"
        "auth": "PASS" | "FAIL" | "TIMEOUT",
        "authError": string?,        // FAIL 时的错误码或消息
        "availableModels": string[], // 完整模型列表
        "latencyMs": number | null
      }
    ]
  }
```

`PRECHECK` source 仅出现在响应里供日志/审计使用，**实际处理时跳过持久化**（参见 §5）。

### 后端实现要点

```java
@Service
@RequiredArgsConstructor
public class ChannelHealthService {

    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway credentialGateway;
    private final ConnectivityTester connectivityTester;
    private final Executor healthCheckExecutor; // 独立 ThreadPool

    @Transactional
    public ChannelHealthResult check(Long channelId, ChannelHealthSource source) {
        Channel channel = channelGateway.findById(channelId)
            .orElseThrow(() -> new GatewayException("CHANNEL_NOT_FOUND"));
        List<ChannelCredential> credentials = credentialGateway.findByChannelId(channelId);

        Instant startedAt = Instant.now();

        List<CompletableFuture<KeyTestResult>> futures = credentials.stream()
            .map(c -> CompletableFuture.supplyAsync(
                () -> connectivityTester.test(channel, c, Duration.ofSeconds(5)),
                healthCheckExecutor
            ).orTimeout(5, TimeUnit.SECONDS)
             .exceptionally(t -> KeyTestResult.timeout(c, t)))
            .toList();

        // 总超时 30s
        try {
            CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
                .orTimeout(30, TimeUnit.SECONDS)
                .join();
        } catch (Exception ignored) {
            // 超时的 Future 已被 exceptionally 接住，这里忽略 outer timeout
        }

        List<KeyTestResult> results = futures.stream().map(CompletableFuture::join).toList();
        ChannelHealthStatus aggregate = aggregate(results);

        // 仅 CARD / DRAWER 持久化；PRECHECK 跳过
        if (source != ChannelHealthSource.PRECHECK) {
            persistHealth(channel, aggregate, source);
        }

        return ChannelHealthResult.builder()
            .channelId(channelId)
            .aggregateStatus(aggregate)
            .startedAt(startedAt)
            .finishedAt(Instant.now())
            .matrix(results.stream().map(this::toMatrixRow).toList())
            .build();
    }

    static ChannelHealthStatus aggregate(List<KeyTestResult> results) {
        if (results.isEmpty()) return ChannelHealthStatus.UNKNOWN;
        long pass = results.stream().filter(r -> r.auth() == AuthStatus.PASS && !r.availableModels().isEmpty()).count();
        long total = results.size();
        if (pass == total) return ChannelHealthStatus.HEALTHY;
        if (pass == 0)     return ChannelHealthStatus.FAILED;
        return ChannelHealthStatus.DEGRADED;
    }

    private void persistHealth(Channel channel, ChannelHealthStatus status, ChannelHealthSource source) {
        try {
            channel.setLastHealthCheckAt(Instant.now());
            channel.setLastHealthStatus(status);
            channel.setLastHealthSource(source);
            channelGateway.save(channel);
            log.info("健康状态写入: channelId={}, status={}, source={}", channel.getId(), status, source);
        } catch (Exception e) {
            log.error("健康状态写入失败: channelId={}", channel.getId(), e);
            // 主流程不抛出
        }
    }

    private KeyMatrixRow toMatrixRow(KeyTestResult r) {
        return KeyMatrixRow.builder()
            .credentialId(r.credentialId())
            .keyMasked(maskKey(r.apiKeyPlain()))
            .auth(r.auth())
            .authError(r.errorMessage())
            .availableModels(r.availableModels())
            .latencyMs(r.latencyMs())
            .build();
    }
}
```

### 前端

```typescript
const ac = new AbortController();
const result = await axios.post(`/api/channels/${id}/health-check`,
  { source: 'DRAWER' },
  { signal: ac.signal, timeout: 35000 }
);
// 关闭 Drawer 时调 ac.abort()
```

## 4. 健康指示点

```tsx
// ChannelCard.tsx 中状态 Tag 旁
<Space size={6}>
  <Tag color={meta.tagColor}>...</Tag>
  <HealthDot status={channel.lastHealthStatus} />
</Space>
```

```tsx
// HealthDot.tsx
const COLORS = { HEALTHY: '#52c41a', DEGRADED: '#faad14', FAILED: '#ff4d4f', UNKNOWN: '#bfbfbf' };

export function HealthDot({ status, lastCheckAt, source }: Props) {
  const color = COLORS[status ?? 'UNKNOWN'];
  const isUnknown = !status || status === 'UNKNOWN';
  const popoverContent = lastCheckAt
    ? `${t('health.lastCheckAt', { time: dayjs(lastCheckAt).fromNow() })} (${t(`health.source.${source}`)})`
    : t('health.notTested');
  return (
    <Popover content={popoverContent}>
      <span style={{
        display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
        backgroundColor: isUnknown ? 'transparent' : color,
        border: isUnknown ? `1px solid ${color}` : 'none',
      }} />
    </Popover>
  );
}
```

Provider Header 旁的「N/M 健康」聚合：在 `ProviderGroupHeader.tsx` 加一行小字，`N` = HEALTHY 数，`M` = 该 Provider 下渠道总数。

## 5. 创建入口状态机

### State 形态

```typescript
interface QuickOnboardState {
  step: 0 | 1 | 2 | 3;
  // Step 0
  selectedProviderCode: string | null;
  // Step 0.5（同步与 step=0 共存）
  inlineProviderExpanded: boolean;
  inlineProvider: InlineProviderForm | null;
  // Step 1+
  endpoints: EndpointForm[];
  selectedModels: string[];
  apiKeysRaw: string;
}

interface InlineProviderForm {
  code: string;
  name: string;
  description?: string;
  websiteUrl?: string;
  apiDocUrl?: string;
}
```

### 转换规则（不变量）

- `selectedProviderCode != null` ⇔ `inlineProviderExpanded == false && inlineProvider == null`
- `inlineProviderExpanded == true` ⇔ `selectedProviderCode == null`
- 用户切换"使用已有"/"新建"时，clear 对方分支的全部字段
- Step 0 的"下一步"校验：`selectedProviderCode != null` 或 (`inlineProviderExpanded == true && validate(inlineProvider) == ok`)
- 最终提交时构造 payload：

```typescript
const payload: ProvisionFromPlanRequest = {
  endpoints: state.endpoints,
  apiKeys: parseApiKeys(state.apiKeysRaw),
  inlineProvider: state.inlineProvider ?? undefined,
};
await axios.post(`/api/v1/provision/from-plan/${planCode}`, payload);
```

### 取消/退出

- 取消整个 Wizard：纯前端 reset state，无任何 API 调用
- 关闭 Drawer：同上

## 6. 后端事务性 Provision

### DTO 扩展

```java
// ProvisionRequest.java
public class ProvisionRequest {
    private List<String> apiKeys;
    private InlineProvider inlineProvider;  // 新增

    public record InlineProvider(
        String code,             // 必须与 planCode 的 provider_code 匹配（后端校验）
        String name,
        String description,
        String websiteUrl,
        String apiDocUrl
    ) {}
}
```

### Service 改造

```java
@Transactional
public ProvisionResult provisionFromPlan(String planCode, ProvisionRequest request) {
    PlanCatalog catalog = planCatalogGateway.findByPlanCode(planCode)
        .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND", ...));

    // 校验：inlineProvider.code 必须与 catalog.providerCode 一致
    InlineProvider inline = request != null ? request.getInlineProvider() : null;
    if (inline != null && !Objects.equals(inline.code(), catalog.getProviderCode())) {
        throw new CatalogException("INLINE_PROVIDER_CODE_MISMATCH",
            "inlineProvider.code 与套餐 providerCode 不一致");
    }

    Provider provider = ensureProvider(catalog.getProviderCode(), inline);

    // ... 后续不变
}

private Provider ensureProvider(String providerCode, InlineProvider inline) {
    return providerGateway.findByCode(providerCode).orElseGet(() -> {
        Provider p = new Provider();
        p.setCode(providerCode);
        p.setPriority(100);
        if (inline != null) {
            p.setName(inline.name() != null ? inline.name() : providerCode);
            p.setDescription(inline.description());
            p.setWebsiteUrl(inline.websiteUrl());
            p.setApiDocUrl(inline.apiDocUrl());
        } else {
            p.setName(providerCode);
        }
        Provider saved = providerGateway.save(p);
        log.info("自动创建供应商: code={}, inline={}, id={}", providerCode, inline != null, saved.getId());
        return saved;
    });
}
```

### 事务回滚验证

集成测试通过 mock `channelEndpointGateway.save` 在第 N 次调用时抛 `RuntimeException`，断言：
- `providerGateway.findByCode(code)` 返回 empty（即未持久化）
- `channelGateway.findByProviderIdAndName(...)` 返回 empty

## 7. 健康字段持久化与并发

### 实体扩展

```java
// Channel.java
@Column(name = "last_health_check_at")
private Instant lastHealthCheckAt;

@Column(name = "last_health_status", length = 16)
@Enumerated(EnumType.STRING)
private ChannelHealthStatus lastHealthStatus;

@Column(name = "last_health_source", length = 16)
@Enumerated(EnumType.STRING)
private ChannelHealthSource lastHealthSource;

public enum ChannelHealthStatus { HEALTHY, DEGRADED, FAILED, UNKNOWN }
public enum ChannelHealthSource { CARD, DRAWER, PRECHECK }
```

### 迁移

```sql
-- V1__add_channel_health_columns.sql （或合适的版本号）
ALTER TABLE channels
  ADD COLUMN last_health_check_at TIMESTAMP NULL,
  ADD COLUMN last_health_status VARCHAR(16) NULL,
  ADD COLUMN last_health_source VARCHAR(16) NULL;

CREATE INDEX idx_channels_last_health_status ON channels(last_health_status);
```

### 并发：last-write-wins

不加 `@Version` 乐观锁。两个测试同时进行时，DB UPDATE 的最后执行者胜出；timestamp 字段反映最后落库顺序，符合 spec scenario "并发测试采用 last-write-wins"。

## 8. 危险操作复用

```typescript
// gateway-console/src/components/common/useDangerConfirm.ts

interface DangerConfirmOptions {
  titleKey: string;
  descriptionKey: string;
  descriptionParams?: Record<string, unknown>;
  onOk: () => void | Promise<void>;
}

export function useDangerConfirm() {
  const { t } = useTranslation();
  const [modal, contextHolder] = Modal.useModal();
  const confirm = useCallback((opts: DangerConfirmOptions) => {
    modal.confirm({
      title: t(opts.titleKey),
      content: t(opts.descriptionKey, opts.descriptionParams),
      okType: 'danger',
      okText: t('common.delete'),
      cancelText: t('common.cancel'),
      onOk: opts.onOk,
    });
  }, [modal, t]);
  return { confirm, contextHolder };
}
```

调用方：

```tsx
const { confirm, contextHolder } = useDangerConfirm();
return (
  <>
    {contextHolder}
    <Button onClick={() => confirm({
      titleKey: 'credential.deleteTitle',
      descriptionKey: 'credential.deleteDescription',
      descriptionParams: { keyMasked: cred.keyMasked },
      onOk: () => deleteMutation.mutateAsync(cred.id),
    })}>
      删除
    </Button>
  </>
);
```

暂停操作仍用 `Popconfirm`（轻量级、可恢复），不复用此 hook。

## 9. 测试策略

### 后端

| 测试类 | 范围 |
|---|---|
| `ChannelHealthServiceTest` | 聚合规则 4 分支（HEALTHY/DEGRADED/FAILED/UNKNOWN）+ PRECHECK 不持久化 + 持久化失败不抛 |
| `ChannelHealthRepositoryTest` | 三新字段读写 + 索引存在性（H2） |
| `ChannelHealthControllerIT` | POST /health-check 端到端：三种 source / 零 Key / 并发触发 last-write-wins |
| `ChannelProvisionServiceTest` | `ensureProvider` 三路径（providerCode 不存在+inline / 不存在+无 inline / 已存在） + code-mismatch 异常 |
| `ChannelProvisionTransactionalIT` | 内联创建中途强制抛 RuntimeException → 断言 Provider 与 Channel 均未持久化 |

### 前端

**测试栈引入**（独立 task group）：
```bash
pnpm add -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom @vitest/ui @playwright/test
```
配置 `vite.config.ts` 的 test 段、`vitest.setup.ts`、`playwright.config.ts`，添加 `package.json` scripts (`test`, `test:e2e`)。

| 测试类 | 范围 |
|---|---|
| `lifecycle.test.ts` | CHANNEL_LIFECYCLE 字段不变性 + 5 个 selector 函数 |
| `useSavePulse.test.tsx` | className 切换 / 3 秒后清除 success / error 持续 / cleanup |
| `useDangerConfirm.test.tsx` | 调用 confirm → modal.confirm 被调用 / onOk 异步 |
| `ChannelStateTag.test.tsx` | 5 状态 Tooltip 内容 |
| `HealthDot.test.tsx` | 4 状态颜色 + UNKNOWN 空心 + Popover 内容 |
| `EndpointSection.test.tsx` | mutation 成功 → triggerSuccess / 失败 → triggerError + 回滚 |
| `CredentialSection.test.tsx` | 同上 |
| `QuickOnboardMode.test.tsx` | 状态机：选已有 / 展开新建 / 切换分支 clear / 提交 payload 含 inlineProvider |

**E2E (Playwright) 3 条**：
- `e2e/onboard-inline-provider.spec.ts` (S1)：完整内联创建路径，断言主页面无独立"+ 新增供应商"按钮
- `e2e/health-check-matrix.spec.ts` (S5)：从卡片闪电图标 → 跳转抽屉 → 测试矩阵 → 关闭后卡片显示健康指示点
- `e2e/delete-key-confirm.spec.ts` (S6)：删除 Key 弹出 Modal.confirm（含 description "删除后无法恢复"）

## 10. 实施顺序（与 tasks.md 对齐）

1. **后端先行**（task group 1-3）：DB 迁移 → 实体字段 → 聚合服务 → API → DTO 扩展 → 事务 Provision → 后端测试
2. **前端测试栈引入**（新增 task group 4'）：Vitest + RTL + Playwright + 配置 + smoke test
3. **前端低风险批**（task group 4-5）：错误反馈兜底 → 状态 SSOT 整合 + Tooltip + RETIRED 重设
4. **前端反馈批**（task group 6）：useSavePulse + 接入 4 个 Section
5. **前端确认批**（task group 7）：useDangerConfirm + 5 处替换 + 暂停 Popconfirm
6. **前端测试归一批**（task group 8）：闪电图标改造 + 矩阵 Table + ConnectivityTestPanel 改名 + 健康指示点
7. **前端创建合并批**（task group 9）：ProviderForm 拆分 + Step 0.5 内联 + 主页面按钮移除
8. **国际化与联调**（task group 10-11）：i18n 整理 → 9 条验收场景联调 → E2E 3 条 → 验证报告

## 11. 已识别的开放问题（推迟）

- 健康指示点 + Provider Header 聚合是否冗余 → 视觉走查阶段决定，不阻塞开发
- 后端 ConnectivityTester 是否已存在统一封装 → build 阶段开始时 codegraph 确认；如不存在按现有 `ConnectivityTestService` 包装
- 矩阵 Table 在 < 1024px 下的折叠规则 → 留 Phase 3 响应式专项

## 引用

- 业务上下文：`openspec/changes/channel-ux-overhaul/proposal.md`、`design.md`
- 验收 scenario：`openspec/changes/channel-ux-overhaul/specs/channel-console-ux/spec.md`、`channel-health-tracking/spec.md`、`channel-provision/spec.md`
- brainstorming 决策落点：`openspec/changes/channel-ux-overhaul/.comet/handoff/brainstorm-summary.md`
