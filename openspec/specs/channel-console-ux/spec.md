# channel-console-ux Specification

## Purpose
TBD - created by archiving change channel-ux-overhaul. Update Purpose after archive.
## Requirements
### Requirement: 渠道创建入口单一闭合

控制台主页面 SHALL 仅暴露一个渠道接入入口（"+ 新增渠道"按钮），渠道创建向导第一步 SHALL 同时支持选择已有供应商和在同一 Drawer 内联创建新供应商。

#### Scenario: 选择已有供应商进入创建
- **WHEN** 用户点击主页面"+ 新增渠道"按钮，并在第一步选择一个已有供应商
- **THEN** 向导直接进入端点配置步骤，无需跳出 Drawer

#### Scenario: 内联创建供应商后接续创建渠道
- **WHEN** 用户在向导第一步点击"+ 新建供应商"链接，填写并提交供应商表单
- **THEN** 向导在同一 Drawer 内完成供应商创建，并自动选中新建供应商进入端点配置步骤

#### Scenario: 内联创建中途取消
- **WHEN** 用户在内联创建供应商成功后取消向导
- **THEN** 系统通过事务性 API 确保不留下孤儿供应商（已通过 channel-provision 能力保障）

#### Scenario: 主页面不再暴露独立的供应商创建入口
- **WHEN** 用户访问 /channels 主页面
- **THEN** 顶部工具栏不存在独立的"+ 新增供应商"按钮，仅保留"+ 新增渠道"、"批量导入"、"批量导出"

### Requirement: 字段保存反馈可视化

渠道详情抽屉内所有支持即时保存的字段（端点、凭证、模型映射），SHALL 在保存成功后显示视觉反馈，SHALL 在保存失败时显示明确的错误反馈。

#### Scenario: 即时保存成功后的视觉脉冲
- **WHEN** 用户在端点/凭证/模型映射 Section 行内编辑某个字段并触发保存，且后端返回成功
- **THEN** 该行短暂显示绿色背景脉冲（约 800ms），行尾出现"✓ 已保存"标记并在 3 秒内淡出

#### Scenario: 保存失败时的内联错误反馈
- **WHEN** 用户编辑某字段触发保存，且后端返回错误或请求失败
- **THEN** 该行显示红色边框，行尾显示"✗ 保存失败：<原因>"，同时全局 message.error 提示原因，且字段值回滚到上一个已保存值

#### Scenario: 配额批量保存的反馈一致
- **WHEN** 用户在配额 Section 进入编辑模式并点击"保存"
- **THEN** 保存成功后 Section 切回展示模式，并对编辑区触发与即时保存相同的脉冲反馈

### Requirement: 渠道生命周期状态语义可见

渠道生命周期五个状态（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED）的 UI 呈现，SHALL 让用户在不离开当前页面的情况下理解每个状态的业务后果。

#### Scenario: 状态 Tag 提供 Tooltip 说明
- **WHEN** 用户 hover 任意状态 Tag
- **THEN** Tooltip 显示状态名称、状态描述、是否参与流量分配、是否计费、可转换至的下一状态列表

#### Scenario: DEPRECATED 状态的特殊提示
- **WHEN** 渠道卡片或详情显示状态为 DEPRECATED
- **THEN** 该卡片/标题区域包含小字说明"仍参与流量分配，但已标记为不推荐使用"

#### Scenario: RETIRED 状态的视觉处理
- **WHEN** 渠道卡片显示状态为 RETIRED
- **THEN** 卡片不再使用 opacity 0.5 整体降透，而是渠道名称加 line-through 样式、文字颜色保证 WCAG AA 对比度（≥ 4.5:1）

#### Scenario: 状态配置的单一来源
- **WHEN** 任何组件需要读取状态的颜色、文案、是否可路由、是否计费、可转换状态信息
- **THEN** 这些信息从单一的状态配置 SSOT 派生，不允许在多个文件中重复定义状态属性

### Requirement: 连通性测试入口归一

渠道连通性测试的执行入口 SHALL 集中到详情抽屉，列表卡片的测试图标 SHALL 仅作为快捷跳转引导，供应商级别的"预检工具"SHALL 与已建渠道明确解耦。

#### Scenario: 卡片闪电图标作为跳转引导
- **WHEN** 用户在渠道列表卡片点击闪电图标
- **THEN** 系统打开该渠道的详情抽屉，自动切换到 Credentials Tab，并对"测试全部"按钮做 800ms 高亮提示

#### Scenario: 详情抽屉的测试矩阵展现
- **WHEN** 用户在详情抽屉点击"连通性测试"
- **THEN** 测试结果以矩阵 Table 展现：每行一个 Key（脱敏显示），列包括认证状态、可用模型数、延迟、测试时间戳

#### Scenario: 预检工具与已建渠道脱耦
- **WHEN** 用户从供应商分组菜单打开"预检工具"
- **THEN** UI 文案明确告知"用于在创建渠道前测试 baseUrl + Key 的可用性"，且测试结果不写入任何已建渠道的健康状态字段

#### Scenario: 列表卡片显示最近一次健康状态
- **WHEN** 渠道有至少一次连通性测试记录
- **THEN** 列表卡片在状态 Tag 旁渲染健康指示点，hover 显示"最后一次测试：<时间> 来源：<卡片/详情/预检>"

#### Scenario: 预检工具的测试结果不持久化
- **WHEN** 用户从供应商分组菜单的"预检工具"完成连通性测试
- **THEN** 系统不写入任何已建渠道的 last_health_check_at / last_health_status / last_health_source 字段

### Requirement: 危险操作确认强度对齐

渠道相关的高影响操作（暂停、各类删除）SHALL 提供与其影响强度相匹配的二次确认。

#### Scenario: 暂停操作的轻量确认
- **WHEN** 用户点击渠道的"暂停"操作（任意入口）
- **THEN** 系统弹出 Popconfirm，文案明确告知"暂停后该渠道不再分配流量，但保留配置"，用户确认后才执行状态转换

#### Scenario: 删除 API Key 的强确认
- **WHEN** 用户点击 API Key 行的删除按钮
- **THEN** 系统弹出 Modal.confirm（红色 danger okType），description 明确"删除后无法恢复，使用此 Key 的请求将立即失败"

#### Scenario: 删除端点的强确认
- **WHEN** 用户点击端点行的删除按钮
- **THEN** 系统弹出 Modal.confirm（红色 danger okType），description 明确删除该端点后对路由的影响

#### Scenario: 删除模型映射的强确认
- **WHEN** 用户点击模型映射行的删除按钮
- **THEN** 系统弹出 Modal.confirm（红色 danger okType），description 明确删除映射后该模型 ID 不再被路由到此渠道

#### Scenario: 删除整个渠道的确认
- **WHEN** 用户在卡片或详情抽屉触发删除渠道操作
- **THEN** 系统弹出 Modal.confirm（红色 danger okType），description 包含"删除后无法恢复"及对运行流量的影响说明

### Requirement: 错误反馈不变量

渠道相关页面所有 mutation 操作 SHALL 满足"错误必反馈"不变量，不得静默吞掉异常。

#### Scenario: mutation 失败必有用户可见反馈
- **WHEN** 任意 mutation（创建、更新、删除、测试）抛出异常或后端返回错误
- **THEN** 用户必须收到至少一种以下反馈：行内错误标记、message.error 全局提示，或两者皆有

#### Scenario: 校验失败的反馈
- **WHEN** 表单校验失败（前端或后端校验）
- **THEN** 失败字段必须显示校验错误信息，不得仅在控制台打印或注释中标注

