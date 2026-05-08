# Git 工作流指南

本文档记录项目的 Git 工作流程和常用操作。

## 分支策略

- **master**: 主分支，生产环境代码
- **feature/xxx**: 功能开发分支
- **refactor/xxx**: 重构分支
- **fix/xxx**: Bug 修复分支

## 常用操作

### 1. 创建并切换到新分支

```bash
git checkout -b feature/new-feature
```

### 2. 提交代码

```bash
git add .
git commit -m "feat: 添加新功能"
git push origin feature/new-feature
```

### 3. 合并分支到 master

```bash
git checkout master
git merge feature/new-feature
git push origin master
```

## Gitee 创建 Pull Request

### 方式一：通过 API 创建 PR

使用 Gitee REST API v5 可以直接创建 Pull Request。

#### 1. 配置认证

首先配置 Git credentials（推荐方式）：

```bash
echo "https://用户名:私人令牌@gitee.com" > ~/.git-credentials
```

获取私人令牌：Gitee → 设置 → 私人令牌 → 生成新令牌

#### 2. 创建 PR 命令

```bash
curl -X POST "https://用户名:私人令牌@gitee.com/api/v5/repos/用户名/仓库名/pulls" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "PR标题",
    "head": "源分支名",
    "base": "目标分支名",
    "body": "PR描述内容"
  }'
```

#### 3. 实际示例

```bash
curl -X POST "https://ezxbao_liuye:YOUR_TOKEN@gitee.com/api/v5/repos/ezxbao_liuye/llm-gateway/pulls" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "refactor(architecture): 重构异常和拦截器架构",
    "head": "refactor/entity-id-reference",
    "base": "master",
    "body": "## 概述\n\n重构异常和拦截器架构。\n\n## 测试\n- ✅ 所有单元测试通过"
  }'
```

#### 4. API 返回结果

成功创建后返回 JSON，包含：
- `html_url`: PR 页面链接
- `number`: PR 编号
- `state`: PR 状态（open/closed/merged）
- `mergeable`: 是否可合并

### 方式二：通过 Web 界面创建

访问仓库页面 → Pull Requests → 新建 Pull Request

PR 创建链接格式：
```
https://gitee.com/用户名/仓库名/pull/new/用户名?源分支..目标分支
```

## 提交消息规范

遵循 Conventional Commits 格式：

```
<type>: <description>

<optional body>
```

### 类型（type）

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| refactor | 重构（不改变功能） |
| docs | 文档更新 |
| test | 测试相关 |
| chore | 构建/工具相关 |
| perf | 性能优化 |
| ci | CI/CD 相关 |

### 示例

```
feat: 添加用户认证功能

fix: 修复登录页面样式问题

refactor(architecture): 重构异常处理架构以符合 COLA 规范
```

## 常见问题

### Q: 如何查看远程仓库配置？

```bash
git remote -v
```

### Q: 如何撤销未提交的更改？

```bash
git restore <file>
```

### Q: 如何查看提交历史？

```bash
git log --oneline -10
```

### Q: API 创建 PR 返回 "Not Found Project"？

原因：认证信息缺失或令牌无效

解决：
1. 检查 `~/.git-credentials` 文件是否存在
2. 确认令牌格式正确：`https://用户名:令牌@gitee.com`
3. 确认令牌权限包含 `projects` 权限

## 参考链接

- [Gitee API v5 文档](https://gitee.com/api/v5/swagger)
- [Conventional Commits](https://www.conventionalcommits.org/)