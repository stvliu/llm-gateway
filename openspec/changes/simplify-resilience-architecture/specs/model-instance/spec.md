# Model Instance Delta Spec

## REMOVED Requirements

### Requirement: 路由优先级在 ModelInstance 级别
**Reason**: 转移顺序改由应用级 `ApplicationChannel.priority` 承载。同一渠道对不同应用地位不同（渠道A 对客服是主用、对内部工具是备用），全局 `ModelInstance.priority` 无法表达这种应用级差异。应用级 priority 完全取代全局，ModelInstance.priority 退场，避免两套 priority 埋坑。
**Migration**: `ModelInstance.priority` 字段删除，`model_instances.priority` 列删除。`PriorityRouter` 排序键改为 `ApplicationChannel.priority`。`InstanceSelector` 的 `findActiveByModelIdOrderByPriority` 改为按应用级 priority 排序（经 ApplicationChannel 注入）。
