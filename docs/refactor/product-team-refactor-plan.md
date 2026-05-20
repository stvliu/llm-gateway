# Product + Team 重构计划

> 本文档描述增量重构方案，引入 Product 概念和 Team 体系，同时保持向后兼容。

## 重构原则

1. **增量演进**：每个阶段可独立部署，不破坏现有功能
2. **数据兼容**：新字段可为空或带默认值，旧数据自动迁移
3. **API 稳定**：v1 API 保持不变，内部逻辑逐步切换
4. **可回滚**：每个阶段提供回滚脚本

---

## 阶段 1：新增 Product 层

### 1.1 目标

在 Provider 和 Model 之间引入 Product 概念，实现产品类型区分和多协议端点支持。

### 1.2 实体变更

#### 新增实体

```
domain/product/
├── entity/
│   ├── Product.java
│   └── ProductApiKey.java
├── enums/
│   ├── ProductType.java
│   └── ProductState.java
├── gateway/
│   ├── ProductGateway.java
│   └── ProductApiKeyGateway.java
├── service/
│   └── ProductDomainService.java
└── exception/
    └── ProductNotFoundException.java
```

#### 修改实体

**Model.java** 新增字段：
```java
private Long productId;  // 新增：关联产品（可为空，兼容旧数据）
private Long providerId; // 保留：向后兼容
```

### 1.3 数据库迁移

**V16__add_product_tables.sql**：

```sql
-- 1. 创建产品表
CREATE TABLE products (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    provider_id BIGINT NOT NULL REFERENCES providers(id),
    name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL DEFAULT 'pay_as_you_go',
    models JSONB DEFAULT '[]',
    endpoints JSONB DEFAULT '{}',
    quota_limit BIGINT,
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(provider_id, name)
);

-- 2. 创建产品 API Key 表
CREATE TABLE product_api_keys (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    name VARCHAR(128),
    api_key_encrypted TEXT NOT NULL,
    weight INT DEFAULT 1,
    priority INT DEFAULT 1,
    status VARCHAR(16) DEFAULT 'active',
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 3. Model 表新增 productId 字段
ALTER TABLE models ADD COLUMN product_id BIGINT REFERENCES products(id);

-- 4. 自动迁移：为每个 Provider 创建默认产品
INSERT INTO products (provider_id, name, product_type, status)
SELECT id, name || '-default', 'pay_as_you_go', 'active'
FROM providers;

-- 5. 自动迁移：关联 Model 到默认产品
UPDATE models m
SET product_id = p.id
FROM products p
WHERE m.provider_id = p.provider_id
  AND p.name LIKE '%-default';

-- 6. 迁移 ProviderApiKey 到 ProductApiKey
INSERT INTO product_api_keys (product_id, name, api_key_encrypted, weight, priority, status, last_used_at, created_at)
SELECT p.id, pak.key_name, pak.api_key_encrypted, pak.weight, pak.priority, 
       LOWER(pak.state)::VARCHAR, pak.last_used_at, pak.created_at
FROM provider_api_keys pak
JOIN products p ON p.provider_id = pak.provider_id
WHERE p.name LIKE '%-default';

-- 7. 创建索引
CREATE INDEX idx_products_provider ON products(provider_id);
CREATE INDEX idx_products_type ON products(product_type);
CREATE INDEX idx_product_api_keys_product ON product_api_keys(product_id);
CREATE INDEX idx_models_product ON models(product_id);
```

**V17__add_product_to_user_api_keys.sql**（预留）：
```sql
-- 为后续 UserApiKey 关联 Product 预留字段
ALTER TABLE gateway_api_keys ADD COLUMN product_id BIGINT REFERENCES products(id);
```

### 1.4 代码变更

#### 领域层

**Product.java**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class Product extends BaseEntity {
    private Long providerId;
    private String name;
    private ProductType productType;
    private List<String> models;      // JSONB 映射
    private Map<String, String> endpoints; // JSONB 映射
    private Long quotaLimit;
    private ProductState state;
}
```

**ProductApiKey.java**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ProductApiKey extends BaseEntity {
    private Long productId;
    private String name;
    private String apiKeyEncrypted;
    private Integer weight;
    private Integer priority;
    private ProductApiKeyState state;
    private Instant lastUsedAt;
}
```

#### 应用层

**ProductService.java**：
```java
public interface ProductService {
    Product createProduct(CreateProductRequest request);
    Product updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    Product getProduct(Long id);
    Page<Product> listProducts(ProductQuery query);
    
    // 产品密钥管理
    ProductApiKey addApiKey(Long productId, AddApiKeyRequest request);
    void removeApiKey(Long apiKeyId);
    List<ProductApiKey> listApiKeys(Long productId);
}
```

#### 适配层

**ProductController.java**：
```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    // CRUD 端点
}
```

### 1.5 兼容性处理

```java
// ModelGatewayImpl.java
@Override
public Optional<Model> findByProviderModelId(String providerModelId) {
    // 优先按 productId 查询
    // 若 productId 为空，回退到 providerId 查询
    var byProduct = modelRepository.findByProviderModelIdAndProductIsNotNull(providerModelId);
    if (byProduct.isPresent()) {
        return byProduct;
    }
    return modelRepository.findByProviderModelId(providerModelId).map(this::toEntity);
}
```

### 1.6 验证清单

- [ ] 产品 CRUD 功能正常
- [ ] 产品密钥 CRUD 功能正常
- [ ] 旧 Model 数据自动关联到默认产品
- [ ] 旧 ProviderApiKey 数据迁移到 ProductApiKey
- [ ] 现有 API 调用不受影响
- [ ] 回滚脚本可用

### 1.7 回滚脚本

**V16__rollback.sql**：
```sql
DROP TABLE IF EXISTS product_api_keys;
DROP TABLE IF EXISTS products;
ALTER TABLE models DROP COLUMN IF EXISTS product_id;
ALTER TABLE gateway_api_keys DROP COLUMN IF EXISTS product_id;
```

---

## 阶段 2：新增 Team 体系

### 2.1 目标

引入 Team 概念，实现用户-团队多对多关系，支持细粒度权限控制。

### 2.2 实体变更

#### 新增实体

```
domain/team/
├── entity/
│   ├── Team.java
│   └── UserTeam.java
├── enums/
│   └── TeamRole.java
├── gateway/
│   ├── TeamGateway.java
│   └── UserTeamGateway.java
├── service/
│   └── TeamDomainService.java
└── exception/
    └── TeamNotFoundException.java
```

#### 修改实体

**UserApiKey.java**（新建，替代 GatewayApiKey）：
```java
@Data
@DomainEntity
public class UserApiKey extends BaseEntity {
    private Long teamId;
    private Long ownerUserId;
    private Long productId;      // 绑定产品
    private String keyHash;
    private String keyPrefix;
    private String name;
    private List<String> models; // 可访问模型子集
    private Long quotaLimit;
    private UserApiKeyState state;
}
```

### 2.3 数据库迁移

**V18__add_team_tables.sql**：

```sql
-- 1. 创建团队表
CREATE TABLE teams (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. 创建用户-团队关联表
CREATE TABLE user_teams (
    user_id BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    role VARCHAR(32) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, team_id)
);

-- 3. 创建用户 API Key 表
CREATE TABLE user_api_keys (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    owner_user_id BIGINT REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    key_hash VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    models JSONB,
    quota_limit BIGINT,
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 4. 自动迁移：为每个用户创建默认团队
INSERT INTO teams (name, description, status)
SELECT username || '-team', 'Default team for ' || username, 'active'
FROM users;

-- 5. 自动迁移：关联用户到默认团队
INSERT INTO user_teams (user_id, team_id, role)
SELECT u.id, t.id, 'owner'
FROM users u
JOIN teams t ON t.name = u.username || '-team';

-- 6. 自动迁移：GatewayApiKey 迁移到 UserApiKey
INSERT INTO user_api_keys (team_id, owner_user_id, product_id, key_hash, key_prefix, name, status, created_at)
SELECT 
    t.id,
    gak.user_id,
    p.id,  -- 关联到用户对应 Provider 的默认产品
    gak.key_hash,
    SUBSTRING(gak.key_encrypted, 1, 10) as key_prefix,
    gak.name,
    LOWER(gak.state)::VARCHAR,
    gak.created_at
FROM gateway_api_keys gak
JOIN users u ON u.id = gak.user_id
JOIN teams t ON t.name = u.username || '-team'
JOIN products p ON p.provider_id IN (SELECT id FROM providers LIMIT 1);

-- 7. 创建索引
CREATE INDEX idx_user_teams_user ON user_teams(user_id);
CREATE INDEX idx_user_teams_team ON user_teams(team_id);
CREATE UNIQUE INDEX idx_user_api_keys_key_hash ON user_api_keys(key_hash);
CREATE INDEX idx_user_api_keys_team ON user_api_keys(team_id);
CREATE INDEX idx_user_api_keys_product ON user_api_keys(product_id);
```

### 2.4 代码变更

#### 领域层

**Team.java**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class Team extends BaseEntity {
    private String name;
    private String description;
    private TeamState state;
}
```

**UserTeam.java**：
```java
@Data
@DomainEntity
public class UserTeam {
    private Long userId;
    private Long teamId;
    private TeamRole role;
}
```

#### 应用层

**TeamService.java**：
```java
public interface TeamService {
    Team createTeam(CreateTeamRequest request);
    void addMember(Long teamId, Long userId, TeamRole role);
    void removeMember(Long teamId, Long userId);
    List<Team> getUserTeams(Long userId);
    
    // 用户密钥管理
    UserApiKey createApiKey(Long teamId, CreateApiKeyRequest request);
    void revokeApiKey(Long apiKeyId);
    List<UserApiKey> listTeamApiKeys(Long teamId);
}
```

### 2.5 验证清单

- [ ] 团队 CRUD 功能正常
- [ ] 用户-团队关联功能正常
- [ ] 用户密钥 CRUD 功能正常
- [ ] 旧 GatewayApiKey 数据迁移到 UserApiKey
- [ ] 现有 API 调用不受影响
- [ ] 回滚脚本可用

### 2.6 回滚脚本

**V18__rollback.sql**：
```sql
DROP TABLE IF EXISTS user_api_keys;
DROP TABLE IF EXISTS user_teams;
DROP TABLE IF EXISTS teams;
```

---

## 阶段 3：切换路由逻辑

### 3.1 目标

将请求路由从 Model-Provider 模式切换到 UserApiKey-Product 模式。

### 3.2 核心变更

#### 认证逻辑

**原逻辑**：
```
请求 → GatewayApiKey.keyHash → User → 权限判断
```

**新逻辑**：
```
请求 → UserApiKey.keyHash → Team → Product → 权限判断
```

#### 路由逻辑

**原逻辑**：
```
Model.providerId → Provider → ProviderApiKey → 调用 API
```

**新逻辑**：
```
UserApiKey.productId → Product → ProductApiKey + Endpoint → 调用 API
```

### 3.3 代码变更

#### 认证服务

**AuthenticationDomainService.java** 修改：
```java
public AuthenticationResult authenticate(String apiKey) {
    // 1. 先查 UserApiKey（新逻辑）
    Optional<UserApiKey> userApiKey = userApiKeyGateway.findByKeyHash(hash(apiKey));
    if (userApiKey.isPresent()) {
        return authenticateByUserApiKey(userApiKey.get());
    }
    
    // 2. 回退到 GatewayApiKey（旧逻辑，兼容）
    Optional<GatewayApiKey> gatewayApiKey = gatewayApiKeyGateway.findByKeyHash(hash(apiKey));
    if (gatewayApiKey.isPresent()) {
        return authenticateByGatewayApiKey(gatewayApiKey.get());
    }
    
    throw new AuthenticationException("Invalid API key");
}
```

#### 路由服务

**ChannelRoutingService.java** 重构：
```java
public RoutingContext resolve(UserApiKey userApiKey, String modelName, String protocol) {
    // 1. 获取产品
    Product product = productGateway.findById(userApiKey.getProductId())
        .orElseThrow(() -> new ProductNotFoundException(userApiKey.getProductId()));
    
    // 2. 验证模型权限
    if (userApiKey.getModels() != null && !userApiKey.getModels().contains(modelName)) {
        throw new ModelAccessDeniedException(modelName);
    }
    
    // 3. 验证模型属于产品
    if (!product.getModels().contains(modelName)) {
        throw new ModelNotInProductException(modelName, product.getName());
    }
    
    // 4. 选择端点
    String endpoint = product.getEndpoints().get(protocol);
    if (endpoint == null) {
        endpoint = product.getEndpoints().get("openai"); // 降级
    }
    
    // 5. 选择密钥
    ProductApiKey apiKey = selectApiKey(product.getId());
    
    // 6. 获取供应商
    Provider provider = providerGateway.findById(product.getProviderId())
        .orElseThrow(() -> new ProviderNotFoundException(product.getProviderId()));
    
    return new RoutingContext(product, provider, apiKey, endpoint, modelName);
}
```

### 3.4 兼容性处理

```java
// 双写策略：创建密钥时同时写入新旧表
@Transactional
public UserApiKey createApiKey(Long teamId, CreateApiKeyRequest request) {
    // 1. 创建 UserApiKey
    UserApiKey userApiKey = new UserApiKey();
    // ... 设置字段
    userApiKey = userApiKeyGateway.save(userApiKey);
    
    // 2. 同时创建 GatewayApiKey（兼容期）
    GatewayApiKey legacyKey = new GatewayApiKey();
    legacyKey.setKeyHash(userApiKey.getKeyHash());
    legacyKey.setUserId(userApiKey.getOwnerUserId());
    // ... 其他字段
    gatewayApiKeyGateway.save(legacyKey);
    
    return userApiKey;
}
```

### 3.5 验证清单

- [ ] 新 API Key 认证流程正常
- [ ] 旧 API Key 仍然可用
- [ ] 模型权限校验正常
- [ ] 产品端点选择正常
- [ ] 产品密钥选择正常
- [ ] 用量统计正常

---

## 阶段 4：清理废弃代码

### 4.1 目标

移除过渡期代码，完成架构升级。

### 4.2 删除内容

| 类型 | 文件/表 | 原因 |
|------|---------|------|
| 实体 | `GatewayApiKey.java` | 已被 `UserApiKey` 替代 |
| 实体 | `ProviderApiKey.java` | 已被 `ProductApiKey` 替代 |
| 表 | `gateway_api_keys` | 数据已迁移 |
| 表 | `provider_api_keys` | 数据已迁移 |
| 字段 | `Model.providerId` | 已迁移到 `Product` |
| 字段 | `GatewayApiKey.*` | 整表删除 |

### 4.3 数据库迁移

**V19__cleanup_legacy_tables.sql**：
```sql
-- 1. 备份数据（可选）
CREATE TABLE gateway_api_keys_backup AS SELECT * FROM gateway_api_keys;
CREATE TABLE provider_api_keys_backup AS SELECT * FROM provider_api_keys;

-- 2. 删除旧表
DROP TABLE IF EXISTS gateway_api_keys;
DROP TABLE IF EXISTS provider_api_keys;

-- 3. 删除 Model 表的 providerId 字段
ALTER TABLE models DROP COLUMN IF EXISTS provider_id;
```

### 4.4 代码删除

```bash
# 删除废弃实体
rm domain/security/entity/GatewayApiKey.java
rm domain/model/entity/ProviderApiKey.java

# 删除废弃 Gateway
rm domain/security/gateway/GatewayApiKeyGateway.java
rm domain/model/gateway/ProviderApiKeyGateway.java

# 删除废弃 Service
rm application/gatewayapikey/ApiKeyService.java
rm application/providerapikey/ProviderApiKeyService.java

# 删除废弃 Controller
rm adapter/api/GatewayApiKeyController.java
rm adapter/api/ProviderApiKeyController.java
```

### 4.5 验证清单

- [ ] 所有测试通过
- [ ] 无编译错误
- [ ] 无运行时错误
- [ ] 数据完整性验证通过

---

## 时间线

| 阶段 | 预计时间 | 里程碑 |
|------|---------|--------|
| 阶段 1 | 1 周 | Product 层上线，旧功能不受影响 |
| 阶段 2 | 1 周 | Team 体系上线，旧功能不受影响 |
| 阶段 3 | 1 周 | 路由逻辑切换，灰度验证 |
| 阶段 4 | 3 天 | 清理废弃代码，完成重构 |

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 数据迁移失败 | 服务不可用 | 分批迁移 + 回滚脚本 |
| 认证逻辑切换失败 | 用户无法访问 | 双写策略 + 灰度切换 |
| 路由逻辑错误 | 请求失败 | Feature Flag 控制 + 快速回滚 |
| 性能下降 | 响应变慢 | 压测验证 + 缓存优化 |

---

**文档版本**: v1.0
**最后更新**: 2026-05-18
