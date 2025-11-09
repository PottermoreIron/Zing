# Auth-Service DDD重构项目

> 基于Spring Boot 3 + Spring Security 6 + DDD的认证授权中心

---

## 📚 文档导航

| 文档 | 描述 | 适用人群 |
|------|------|----------|
| [DDD-REFACTORING-SPEC.md](./DDD-REFACTORING-SPEC.md) | 📋 完整技术规格文档 | 全员必读 |
| [ADR.md](./ADR.md) | 🎯 架构决策记录 | 架构师、Tech Leader |
| [GATEWAY-INTEGRATION.md](./GATEWAY-INTEGRATION.md) | 🚪 网关集成架构设计 | 网关开发者 |
| [Function.md](./Function.md) | 📝 功能需求清单 | 产品、开发 |

---

## 🎯 核心架构决策

### ✅ 已确认

1. **Spring Security 6.x** - 替代自研安全框架
2. **无Session设计** - 纯JWT Token，支持分布式
3. **时间戳标准化** - 所有时间字段使用Unix时间戳
4. **多用户域支持** - Member/Admin分离，抽象UserPrincipal
5. **Gateway认证鉴权** - 网关本地验证JWT + Redis Pub/Sub
6. **组织架构预留** - 当前不实现，仅预留扩展接口
7. **RSA非对称加密** - 保护JWT密钥
8. **DDD四层架构** - 领域驱动设计

---

## 🏗️ 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   Client (Web/Mobile)                │
└────────────────────────┬────────────────────────────┘
                         │ JWT Token
                         ▼
┌─────────────────────────────────────────────────────┐
│                    Gateway                           │
│  ┌────────────────────────────────────────────────┐ │
│  │ • JWT本地验证 (RSA公钥)                        │ │
│  │ • Token黑名单检查 (Caffeine + Redis)          │ │
│  │ • 权限预检查                                    │ │
│  │ • 限流控制                                      │ │
│  └────────────────────────────────────────────────┘ │
└──────────┬──────────────────────────┬───────────────┘
           │                          │
           ▼                          ▼
┌──────────────────┐      ┌──────────────────────┐
│  Auth-Service    │      │  Business Services   │
│  ┌────────────┐  │      │  (member-service等)  │
│  │认证授权中心│  │      │                      │
│  └────────────┘  │      └──────────────────────┘
└──────────────────┘

         │
         ▼
┌──────────────────┐
│  Redis           │
│  • Token黑名单   │
│  • 权限缓存      │
│  • Pub/Sub       │
└──────────────────┘
```

### DDD分层架构

```
auth-service/
├── interfaces/              # 接口层
│   ├── rest/               # REST API
│   ├── event/              # 事件监听器
│   └── schedule/           # 定时任务
├── application/            # 应用层
│   ├── service/            # 应用服务 (用例编排)
│   ├── assembler/          # DTO组装器
│   └── command/            # 命令对象
├── domain/                 # 领域层 ⭐核心
│   ├── authentication/     # 认证上下文
│   │   ├── model/
│   │   │   ├── aggregate/  # JwtToken, TokenBlacklist
│   │   │   ├── entity/
│   │   │   └── valueobject/
│   │   ├── service/        # 领域服务
│   │   ├── repository/     # 仓储接口
│   │   └── event/          # 领域事件
│   ├── authorization/      # 授权上下文
│   │   └── model/
│   │       └── aggregate/  # Role, UserRoleAssignment
│   ├── identity/           # 身份管理上下文
│   │   └── model/
│   │       └── aggregate/  # MemberPrincipal, AdminPrincipal
│   ├── organization/       # 组织架构上下文 (预留)
│   └── shared/             # 共享内核
│       ├── valueobject/    # UserPrincipal, UnixTimestamp
│       ├── event/          # DomainEvent
│       └── exception/      # DomainException
└── infrastructure/         # 基础设施层
    ├── persistence/        # 持久化 (MyBatis)
    ├── cache/              # 缓存 (Redis)
    ├── external/           # 外部服务适配器
    └── security/           # Spring Security集成
```

---

## 🔐 认证授权流程

### 注册流程 (多种方式)

#### 方式1: 密码注册
```
1. 用户注册
   POST /auth/register/password
   {
     "username": "john_doe",
     "email": "user@example.com",
     "phone": "+8613800138000",
     "password": "SecurePass123!",
     "userDomain": "MEMBER"
   }

2. Auth-Service处理
   ├─ 验证用户名/邮箱/手机号唯一性
   ├─ 验证密码策略
   ├─ 创建UserRegistration聚合
   ├─ 创建MemberPrincipal (状态: PENDING_VERIFICATION)
   └─ 发送验证邮件

3. 返回注册结果
   {
     "registrationId": "reg-xxx",
     "userId": "user-123",
     "status": "PENDING_VERIFICATION",
     "message": "注册成功，请验证邮箱"
   }

4. 用户验证邮箱
   POST /auth/register/verify-email
   {
     "token": "verification-token-xxx"
   }

5. 账户激活
   MemberPrincipal状态 → ACTIVE
```

#### 方式2: 验证码注册 (快捷注册 + 自动登录)
```
1. 发送验证码
   POST /auth/code/send
   {
     "type": "EMAIL",
     "recipient": "user@example.com",
     "purpose": "REGISTER"
   }

2. 验证码注册
   POST /auth/register/code
   {
     "recipient": "user@example.com",
     "code": "123456",
     "userDomain": "MEMBER"
   }

3. 自动登录返回Token
   {
     "userId": "user-123",
     "username": "user_1699516800",  // 自动生成
     "accessToken": "eyJhbGc...",
     "refreshToken": "eyJhbGc...",
     "status": "ACTIVE"
   }
```

#### 方式3: OAuth2注册 (一键注册 + 自动登录)
```
1. 跳转OAuth2授权
   GET /oauth2/authorize/github?redirect_uri=...

2. 用户授权后回调
   GET /oauth2/callback/github?code=xxx

3. 自动注册并登录
   - 获取OAuth2用户信息
   - 检查email是否已注册
   - 未注册则创建账户
   - 返回Token
```

#### 方式4: 微信扫码注册 (一键注册 + 自动登录)
```
1. 获取二维码
   GET /auth/wechat/qrcode

2. 用户扫码授权

3. 回调处理
   GET /auth/wechat/callback?code=xxx
   
4. 自动注册并登录
   返回Token
```

---

### 登录流程 (多种方式)

#### 方式1: 密码登录
```
1. 用户登录
   POST /auth/login/password
   {
     "identifier": "john_doe",      // 用户名/邮箱/手机号
     "password": "password123",
     "userDomain": "MEMBER",
     "deviceInfo": {
       "deviceId": "uuid-xxxx",
       "deviceType": "WEB",
       "deviceName": "Chrome on macOS"
     }
   }

2. Auth-Service验证
   ├─ 查询用户 (支持用户名/邮箱/手机号)
   ├─ 验证密码 (BCrypt)
   ├─ 检查账户状态
   ├─ 检测异地登录
   ├─ 记录设备信息
   └─ 签发JWT Token

3. 返回Token
   {
     "accessToken": "eyJhbGc...",
     "refreshToken": "eyJhbGc...",
     "accessTokenExpiresAt": 1699520400,
     "refreshTokenExpiresAt": 1702108800,
     "isSuspiciousLogin": false,
     "deviceId": "uuid-xxxx"
   }
```

#### 方式2: 验证码登录
```
1. 发送验证码
   POST /auth/code/send
   {
     "type": "EMAIL",
     "recipient": "user@example.com",
     "purpose": "LOGIN"
   }

2. 验证码登录
   POST /auth/login/code
   {
     "recipient": "user@example.com",
     "code": "123456",
     "userDomain": "MEMBER",
     "deviceInfo": { ... }
   }

3. 返回Token (同密码登录)
```

#### 方式3: OAuth2三方登录
```
1. 跳转授权
   GET /oauth2/authorize/github?redirect_uri=...

2. 用户授权后回调
   GET /oauth2/callback/github?code=xxx

3. 返回Token
```

#### 方式4: 微信扫码登录
```
1. 获取二维码
   GET /auth/wechat/qrcode

2. 用户扫码确认

3. 回调接口
   GET /auth/wechat/callback?code=xxx

4. 返回Token
```

### 刷新Token流程 (含自动续期)

```
1. AccessToken过期
   客户端检测到401错误

2. 使用RefreshToken刷新
   POST /auth/refresh
   {
     "refreshToken": "eyJhbGc...",
     "deviceInfo": { ... }
   }

3. Auth-Service处理
   ├─ 验证RefreshToken
   ├─ 检查黑名单
   ├─ 检查设备状态
   ├─ 生成新AccessToken
   └─ 判断是否续期RefreshToken
       ├─ 距上次刷新 < 7天 → ✅ 续期30天
       └─ 距上次刷新 > 7天 → ❌ 不续期

4. 返回新Token
   {
     "accessToken": "new_access_token",
     "refreshToken": "new_refresh_token",  // 可能是新的
     "accessTokenExpiresAt": 1699524000,
     "refreshTokenExpiresAt": 1702195200,
     "refreshTokenRenewed": true           // 是否续期
   }

5. 客户端更新Token
   如果refreshTokenRenewed=true，需要更新两个Token
```

```
1. 请求携带Token
   GET /api/member/profile
   Headers:
     Authorization: Bearer eyJhbGc...

2. Gateway验证
   ├─ 提取Token
   ├─ 本地验证签名 (RSA公钥)
   ├─ 检查过期时间
   ├─ 黑名单检查 (Redis缓存)
   ├─ 权限预检查
   └─ 转发请求 (添加X-User-Id等Header)

3. Backend Service处理
   从Header中获取用户信息
   X-User-Id: user-123
   X-User-Domain: MEMBER
   X-Authorities: user:read,user:write
```

### 登出流程

```
1. 用户登出
   POST /auth/logout
   Headers:
     Authorization: Bearer eyJhbGc...

2. Auth-Service处理
   ├─ 解析Token获取jti
   ├─ 加入黑名单 (Redis)
   │   Key: auth:token:blacklist:{jti}
   │   TTL: Token剩余有效期
   └─ 发布事件 (Redis Pub/Sub)
       Channel: auth:token:revoked
       Message: {jti}

3. Gateway订阅事件
   ├─ 接收到jti
   └─ 清除本地缓存
```

### 设备管理流程

```
1. 查看在线设备
   GET /auth/devices

2. 返回设备列表
   [
     {
       "deviceId": "uuid-xxx",
       "deviceType": "WEB",
       "deviceName": "Chrome on macOS",
       "lastIpAddress": "123.456.789.0",
       "lastLoginAt": 1699516800,
       "status": "ACTIVE"
     }
   ]

3. 踢出设备
   POST /auth/devices/{deviceId}/kick
   
4. 该设备Token失效
```

---

## 📊 限界上下文

### 1️⃣ 认证上下文 (Authentication)

**核心聚合**:
- `JwtToken` - JWT Token管理 (含自动续期)
- `TokenBlacklist` - Token黑名单
- `UserDevice` - 设备管理
- `VerificationCode` - 验证码管理

**关键功能**:
- 密码认证 (用户名/邮箱/手机号)
- 验证码认证 (邮箱/手机号)
- OAuth2认证 (GitHub/Google)
- 微信扫码认证
- Token签发与刷新 (滑动窗口续期)
- Token黑名单管理
- 设备登录追踪
- 异地登录检测
- 设备踢出功能

### 2️⃣ 授权上下文 (Authorization)

**核心聚合**:
- `Role` - 角色管理
- `UserRoleAssignment` - 用户角色分配

**关键功能**:
- RBAC权限模型
- 角色-权限关联
- 权限检查
- 权限缓存

### 3️⃣ 身份管理上下文 (Identity)

**核心聚合**:
- `MemberPrincipal` - 会员用户
- `UserRegistration` - 用户注册流程管理
- `AdminPrincipal` - 后台用户 (预留)

**关键功能**:
- 用户注册 (密码/验证码/OAuth2/微信)
- 邮箱/手机号验证
- 密码管理
- 账户状态控制
- 登录失败追踪

### 4️⃣ 组织架构上下文 (Organization) - 预留

**扩展接口**:
- `OrganizationalEntity` - 组织实体接口
- `DataScopeProvider` - 数据权限提供者

**未来功能**:
- 部门管理
- 职位体系
- 数据权限

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2+ |
| 安全 | Spring Security | 6.2+ |
| JWT | jjwt | 0.12+ |
| ORM | MyBatis Plus | 3.5+ |
| 缓存 | Redis | 7.0+ |
| 注册中心 | Nacos | 2.3+ |
| 数据库 | MySQL | 8.0+ |
| OAuth2 | Spring Security OAuth2 Client | - |
| 微信 | weixin-java-open | 4.6+ |

---

## 🚀 快速开始

### 环境准备

```bash
# 1. 启动MySQL
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=auth_db \
  mysql:8.0

# 2. 启动Redis
docker run -d -p 6379:6379 redis:7.0

# 3. 启动Nacos
docker run -d -p 8848:8848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.0
```

### 生成RSA密钥对

```bash
cd auth-service/src/main/resources/keys

# 生成私钥
openssl genrsa -out jwt_private_key.pem 2048

# 导出公钥
openssl rsa -in jwt_private_key.pem -pubout -out jwt_public_key.pem
```

### 配置文件

```yaml
# application.yml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379

pot:
  jwt:
    private-key: classpath:keys/jwt_private_key.pem
    public-key: classpath:keys/jwt_public_key.pem
    access-token-ttl: 3600        # 1小时
    refresh-token-ttl: 2592000    # 30天
```

### 启动服务

```bash
# 开发模式
mvn spring-boot:run

# 打包
mvn clean package

# 运行jar
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

---

## 📝 API文档

### 注册接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/auth/register/password` | POST | 密码注册 (用户名+邮箱/手机号+密码) |
| `/auth/register/code` | POST | 验证码注册 (自动登录) |
| `/auth/register/verify-email` | POST | 验证邮箱 |
| `/auth/register/verify-phone` | POST | 验证手机号 |
| `/auth/register/resend-verification` | POST | 重发验证邮件/短信 |

### 认证接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/auth/login/password` | POST | 密码登录 (支持用户名/邮箱/手机号) |
| `/auth/login/code` | POST | 验证码登录 |
| `/auth/logout` | POST | 登出 |
| `/auth/logout/device/{deviceId}` | POST | 踢出指定设备 |
| `/auth/refresh` | POST | 刷新Token (含自动续期) |
| `/auth/validate` | POST | 验证Token (Gateway调用) |

### 验证码接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/auth/code/send` | POST | 发送验证码 (邮箱/手机号) |
| `/auth/code/verify` | POST | 验证验证码 |

### OAuth2接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/oauth2/authorize/{provider}` | GET | OAuth2授权 (GitHub/Google) |
| `/oauth2/callback/{provider}` | GET | OAuth2回调 |

### 微信登录接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/auth/wechat/qrcode` | GET | 获取微信登录二维码 |
| `/auth/wechat/callback` | GET | 微信扫码回调 |

### 设备管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/auth/devices` | GET | 查询用户所有设备 |
| `/auth/devices/{deviceId}/kick` | POST | 踢出指定设备 |

### 用户管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/member/register` | POST | 会员注册 |
| `/member/{id}` | GET | 获取会员信息 |
| `/member/{id}/password` | PUT | 修改密码 |
| `/member/{id}/lock` | POST | 锁定账户 |

### 权限管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/role` | GET/POST | 角色CRUD |
| `/role/{id}/permissions` | POST | 分配权限 |
| `/permission` | GET | 权限列表 |
| `/user/{id}/roles` | POST | 分配角色 |

---

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify -P integration-test
```

### 架构测试

```java
@ArchTest
public static final ArchRule domain_layer_should_not_depend_on_others =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..application..", "..interfaces..");
```

---

## 📈 监控指标

### 关键指标

| 指标 | 目标 | 说明 |
|------|------|------|
| 登录QPS | >1000 | 登录接口吞吐量 |
| 登录延迟 | <200ms (P99) | 登录接口响应时间 |
| JWT验证延迟 | <5ms | Gateway本地验证 |
| 权限检查延迟 | <50ms (P99) | 权限验证响应时间 |
| 缓存命中率 | >95% | 权限缓存命中率 |

### Prometheus监控

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
  metrics:
    tags:
      application: ${spring.application.name}
```

---

## 📅 实施计划

| 阶段 | 工期 | 关键产出 |
|------|------|----------|
| Phase 1: 准备 | 1周 | DDD培训、Spring Security验证 |
| Phase 2: 身份管理 | 2周 | MemberPrincipal聚合 |
| Phase 3: 认证 | 3-4周 | 多种登录方式 + 设备管理 + Token自动续期 |
| Phase 4: 授权 | 3周 | RBAC权限模型 |
| Phase 5: Gateway集成 | 1周 | 网关认证鉴权 |
| Phase 6: 高级功能 | 1-2周 | 监控、安全增强 |
| Phase 7: 测试上线 | 1周 | 灰度发布 |

**总工期**: 约12-14周 (3-3.5个月)

---

## 🤝 贡献指南

### 分支策略

```
main        ← 生产环境
  ↑
develop     ← 开发环境
  ↑
feature/*   ← 功能分支
```

### Commit规范

```
feat: 新功能
fix: 修复bug
refactor: 重构
docs: 文档更新
test: 测试
chore: 构建/工具
```

### Code Review检查项

- [ ] 领域逻辑是否在领域层
- [ ] 是否违反依赖规则
- [ ] 单元测试是否覆盖
- [ ] 是否遵循值对象不可变原则
- [ ] 是否正确使用时间戳

---

## 📞 联系方式

- **项目负责人**: pot
- **邮箱**: yecao.sacu@gmail.com
- **架构咨询**: 参考ADR.md获取架构决策记录

---

## 📄 许可证

本项目采用MIT许可证

---

**最后更新**: 2025-11-09  
**版本**: v1.0.0

