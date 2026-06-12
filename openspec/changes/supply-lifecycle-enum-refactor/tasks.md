## 1. 枚举定义

- [ ] 1.1 在 Channel 实体中新增 Phase 枚举（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED），包含 isRoutable()、isTerminal()、canTransitionTo() 方法
- [ ] 1.2 在 ModelInstance 实体中新增 Phase 枚举（同上）

## 2. 实体修改

- [ ] 2.1 修改 Channel 实体：用 Channel.Phase phase 替换 ChannelState state
- [ ] 2.2 修改 ModelInstance 实体：用 ModelInstance.Phase phase 替换 ChannelModelState state
- [ ] 2.3 修改 Model 实体：去掉 ModelState，新增 deprecatedAt / scheduledRetiredAt / deprecationMessage 字段
- [ ] 2.4 修改 Provider 实体：去掉 ProviderState 字段
- [ ] 2.5 修改 ChannelCredential 实体：去掉 CredentialState 字段
- [ ] 2.6 修改 ChannelEndpoint 实体：去掉 ChannelEndpointState（若存在）

## 3. 旧枚举删除

- [ ] 3.1 删除 ProviderState.java
- [ ] 3.2 删除 ChannelState.java
- [ ] 3.3 删除 ModelState.java
- [ ] 3.4 删除 ChannelModelState.java
- [ ] 3.5 删除 CredentialState.java
- [ ] 3.6 删除 CatalogState.java
- [ ] 3.7 删除 Catalog 实体中的 CatalogState 引用

## 4. 调用方适配

- [ ] 4.1 修改 InstanceSelector：phase.isRoutable() 替代 state == ACTIVE 判断，ACTIVE 优先于 DEPRECATED
- [ ] 4.2 修改 ChannelDomainService：适配 Channel.Phase
- [ ] 4.3 修改 ModelDomainService：适配 Model 无状态字段
- [ ] 4.4 修改 ProviderDomainService：适配 Provider 无状态字段
- [ ] 4.5 修改 ChannelCredentialDomainService：适配无状态字段
- [ ] 4.6 搜索全项目所有引用旧枚举的地方并适配

## 5. 测试

- [ ] 5.1 为 Channel.Phase 编写单元测试（状态转换合法性、isRoutable、isTerminal）
- [ ] 5.2 为 ModelInstance.Phase 编写单元测试
- [ ] 5.3 更新 InstanceSelector 测试

## 6. 数据库迁移

- [ ] 6.1 创建数据库迁移脚本：旧状态值到新 LifecyclePhase 的映射
