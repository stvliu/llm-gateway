# Model Instance Delta Spec

## REMOVED Requirements

### Requirement: 路由优先级在 ModelInstance 级别
**Reason**: 转移顺序改由应用级 `ApplicationChannel.priority` 承载。同一渠道对不同应用地位不同（渠道A 对客服是主用、对内部工具是备用），全局 `ModelInstance.priority` 无法表达这种应用级差异。应用级 priority 完全取代全局，ModelInstance.priority 退场，避免两套 priority 埋坑。
**Migration**: `ModelInstance.priority` 字段删除，`model_instances.priority` 列删除。`PriorityRouter` 排序键改为 `ApplicationChannel.priority`。`InstanceSelector` 的 `findActiveByModelIdOrderByPriority` 改为按应用级 priority 排序（经 ApplicationChannel 注入）。

> **Follow-up（V63 决策拆分）**：`ModelInstance.priority` 字段与 `model_instances.priority` 列的物理删除由用户决策拆为独立后续 change，**本 change 未执行物理删除**。当前实现：`ModelInstance` 实体/`ModelInstanceDo`/`model_instances.priority` 列仍保留 priority 字段（`BuiltinVendorLoader`、`ModelInstanceServiceImpl` 仍读写），但**运行时转移顺序已完全由 `ApplicationChannel.priority` 驱动**（`PriorityRouter` 用 `channelPriorityMap` 精排覆盖 DB 粗排），priority 字段仅作 DB 粗排兜底、不再决定最终转移顺序。物理删除留待后续 change 处理（需评估 `findActiveByModelIdOrderByPriority` 查询与既有数据迁移）。本 spec 保留退场声明，实现以 follow-up 收尾。
