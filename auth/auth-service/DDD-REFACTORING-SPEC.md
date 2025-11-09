# Auth-Service DDD重构技术规格文档

> **版本**: v6.0 Final  
> **日期**: 2025年11月9日  
> **状态**: ✅ 最终确定版本  
> **核心原则**: Auth-Service是**无状态、无数据库**的纯认证授权服务

---

## 📋 目录

1. [需求理解总结](#1-需求理解总结)
2. [核心架构原则](#2-核心架构原则)
3. [系统架构设计](#3-系统架构设计)
4. [限界上下文划分](#4-限界上下文划分)
5. [领域模型设计](#5-领域模型设计)
6. [防腐层设计](#6-防腐层设计)
7. [核心流程设计](#7-核心流程设计)
8. [技术栈选型](#8-技术栈选型)
9. [目录结构设计](#9-目录结构设计)
10. [实施路线图](#10-实施路线图)
11. [风险评估与应对](#11-风险评估与应对)
12. [验收标准](#12-验收标准)

---

## 1. 需求理解总结

### 1.1 当前系统概况

**系统组成**:
- **framework**: 自定义Spring Boot自动装配框架层
  - framework-common: 公共基础组件
  - framework-starter-redis: Redis缓存支持
  - framework-starter-ratelimit: 限流组件
  - framework-starter-touch: 触达通用包(推送、通知)
  
- **member**: 会员领域服务 (C端用户)
  - member-facade: 对外API定义
  - member-service: 会员业务实现
  - **member_db数据库**: 包含用户、角色、权限、设备等所有表
  
- **admin**: 后台管理领域服务 (B端用户) - 未来规划
  - admin-service: 后台用户业务实现
  - **admin_db数据库**: 独立的后台用户数据库
  
- **gateway**: 网关服务 (认证鉴权限流前置)
  - JWT本地验证
  - Token黑名单检查
  - 限流控制

- **auth-service**: 认证授权服务(待重构)
  - ❌ **无数据库**: 完全无状态服务
  - ✅ **依赖**: Spring Security + Redis + Nacos + OpenFeign
  - ✅ **功能**: 纯认证授权逻辑，所有数据通过Feign调用member/admin-service

### 1.2 核心业务能力

**Auth-Service职责** (纯逻辑，无数据存储):
1. **认证管理**: 
   - 多种登录方式 (密码/验证码/OAuth2/微信扫码)
   - JWT Token签发、验证、刷新 (滑动窗口续期)
   - Token黑名单管理 (Redis)
   - 验证码管理 (Redis)

2. **注册流程编排**:
   - 验证码发送与验证
   - 唯一性检查 (调用member-service)
   - 用户创建编排 (调用member-service)
   - OAuth2/微信注册处理

3. **权限查询与缓存**:
   - 调用member-service查询权限
   - 权限结果缓存 (Redis)
   - 权限变更事件处理

4. **设备管理查询**:
   - 调用member-service查询设备列表
   - 设备踢出编排

**Member-Service职责** (数据存储与业务):
1. **用户数据管理**: member_member表
2. **角色权限管理**: member_role, member_permission, member_role_permission, member_member_role表
3. **设备数据管理**: member_device表
4. **密码验证**: BCrypt加密与验证
5. **登录失败追踪**: 失败次数记录与账户锁定

### 1.3 重构目标

将**贫血模型**的传统三层架构重构为**充血模型**的DDD架构，实现:
- ✅ Auth-Service作为**无状态编排层**，纯认证授权逻辑
- ✅ 所有数据存储在member/admin-service
- ✅ 通过Feign实现防腐层，隔离外部服务变更
- ✅ 领域知识显性化，业务逻辑内聚
- ✅ 高内聚低耦合，易于测试和维护

**核心架构决策**:
1. ✅ **Spring Security 6.x** - JWT无状态认证
2. ✅ **无Session、无数据库** - 完全无状态
3. ✅ **时间戳标准化** - Unix时间戳(Long)
4. ✅ **多用户域** - Member/Admin通过UserDomain区分
5. ✅ **Gateway认证鉴权** - 本地JWT验证 + Redis黑名单
6. ✅ **防腐层** - UserModuleAdapter隔离member/admin-service

---

## 2. 核心架构原则

### 2.1 Auth-Service定位

```
Auth-Service = 无状态认证授权编排服务

✅ 负责:
1. 认证逻辑 (登录验证、Token签发)
2. 注册流程编排 (调用member-service创建用户)
3. JWT Token管理 (生成、验证、刷新、黑名单)
4. 验证码管理 (生成、验证、存储Redis)
5. 权限查询编排 (调用member-service + Redis缓存)
6. OAuth2/微信认证集成

❌ 不负责:
1. 用户数据存储 (member-service负责)
2. 角色权限数据存储 (member-service负责)
3. 设备数据存储 (member-service负责)
4. 密码加密存储 (member-service负责)
5. 登录失败追踪存储 (member-service负责)
```

### 2.2 数据存储职责

```
Member-Service (member_db):
✅ member_member                 -- 会员基础信息
✅ member_device                 -- 登录设备
✅ member_social_connections     -- OAuth2/微信绑定
✅ member_role                   -- 角色定义
✅ member_permission             -- 权限定义
✅ member_role_permission        -- 角色权限关联
✅ member_member_role            -- 用户角色分配

Auth-Service:
✅ Redis (临时数据，有TTL)
   - Token黑名单: auth:blacklist:{jti}
   - RefreshToken: auth:refresh:{jti}
   - 验证码: auth:code:{recipient}
   - 权限缓存: auth:permissions:{userId}:{domain}
   - 在线设备缓存: auth:devices:{userId}:{domain}

❌ 无MySQL数据库
```

---

## 3. 系统架构设计

### 3.1 整体架构图

```
┌──────────────────────────────────────────────┐
│                Client                         │
└────────────────┬─────────────────────────────┘
                 │ JWT Token
                 ▼
┌──────────────────────────────────────────────┐
│              Gateway                          │
│  ┌────────────────────────────────────────┐  │
│  │ 1. JWT本地验证 (RSA公钥)               │  │
│  │ 2. Token黑名单检查 (Redis本地缓存)     │  │
│  │ 3. 权限预检查                          │  │
│  │ 4. 限流控制                            │  │
│  └────────────────────────────────────────┘  │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│           Auth-Service                        │
│  ❌ 无MySQL数据库                             │
│  ✅ 只有Redis (临时数据)                      │
│  ┌────────────────────────────────────────┐  │
│  │ Redis存储                               │  │
│  │ - Token黑名单 (TTL=Token剩余有效期)     │  │
│  │ - RefreshToken (TTL=30天)              │  │
│  │ - 验证码 (TTL=5分钟)                    │  │
│  │ - 权限缓存 (TTL=60秒)                   │  │
│  │ - 在线设备缓存 (TTL=10分钟)             │  │
│  └────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────┐  │
│  │ Feign Client (防腐层)                   │  │
│  │ - MemberServiceClient                   │  │
│  │ - AdminServiceClient (预留)             │  │
│  └────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────┐  │
│  │ 领域服务 (纯逻辑，无状态)               │  │
│  │ - AuthenticationDomainService           │  │
│  │ - RegistrationOrchestrationService      │  │
│  │ - PermissionQueryService                │  │
│  └────────────────────────────────────────┘  │
└────────┬───────────────────────┬─────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌──────────────────┐
│ Member-Service  │    │ Admin-Service    │
│ (member_db)     │    │ (admin_db)       │
│ ┌─────────────┐ │    │ ┌──────────────┐ │
│ │所有用户数据 │ │    │ │所有后台数据  │ │
│ │角色权限数据 │ │    │ │角色权限数据  │ │
│ │设备数据     │ │    │ │组织架构数据  │ │
│ └─────────────┘ │    │ └──────────────┘ │
└─────────────────┘    └──────────────────┘
```

### 3.2 依赖关系图

```
┌──────────────────────────────────────┐
│         Auth-Service                  │
│  (无状态、无数据库)                   │
│                                       │
│  依赖:                                │
│  ├─ Spring Security 6.x              │
│  ├─ Redis (临时数据)                 │
│  ├─ OpenFeign (调用member/admin)     │
│  ├─ Nacos (服务发现)                 │
│  └─ framework-starter-*              │
└──────────┬───────────────────┬────────┘
           │                   │
           │                   │
           ▼                   ▼
  ┌────────────────┐   ┌────────────────┐
  │Member-Service  │   │Admin-Service   │
  │(数据持久化)    │   │(数据持久化)    │
  └────────────────┘   └────────────────┘
```

---

## 4. 限界上下文划分

### 4.1 认证上下文 (Authentication Context)

**核心职责**: 用户身份验证与Token管理

**聚合** (值对象，无持久化):
- `JwtToken` - JWT Token值对象
- `TokenBlacklist` - Token黑名单值对象
- `VerificationCode` - 验证码值对象

**领域服务**:
- `AuthenticationDomainService` - 认证逻辑
- `JwtTokenService` - Token生成与验证
- `VerificationCodeService` - 验证码管理

**数据存储**:
- ✅ Redis (临时): Token黑名单、RefreshToken、验证码
- ❌ MySQL: 无

---

### 4.2 注册编排上下文 (Registration Orchestration Context)

**核心职责**: 编排用户注册流程

**领域服务**:
- `RegistrationOrchestrationService` - 注册流程编排
- `UniquenessCheckService` - 唯一性检查编排

**流程**:
1. 接收注册请求
2. 调用member-service检查唯一性
3. 发送验证码 (存Redis)
4. 验证验证码
5. 调用member-service创建用户
6. 可选: 自动登录返回Token

**数据存储**:
- ✅ Redis (临时): 验证码
- ❌ MySQL: 无 (直接调用member-service)
│  ┌──────────────────────────────────────┐   │
│  │  Controllers (推测，待确认)           │   │
│  ├──────────────────────────────────────┤   │
│  │  Services (业务逻辑层)                │   │
│  ├──────────────────────────────────────┤   │
│  │  Mappers/DAOs (数据访问层)            │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │          │            │
         ▼          ▼            ▼
    ┌────────┐  ┌────────┐  ┌──────────────┐
    │framework│ │ member │  │ Nacos/Redis  │
    │ starters│ │ facade │  │              │
    └────────┘  └────────┘  └──────────────┘
```

### 2.3 现状问题识别

**潜在问题**:
1. ❌ **业务逻辑分散**: Service层可能包含大量过程式代码，领域规则分散
2. ❌ **贫血模型**: 实体类可能仅是数据容器，缺乏行为
3. ❌ **跨领域耦合**: 认证、授权、用户、组织等概念可能混杂
4. ❌ **测试困难**: 业务逻辑与基础设施耦合紧密
5. ❌ **扩展性差**: 新增OAuth2提供商、新权限模型需大量改动

---

## 3. DDD重构可行性评估

### 3.1 为什么DDD适合Auth-Service？

#### ✅ **强一致性需求**
- 权限变更需要立即生效(缓存失效)
- 用户锁定/解锁需强一致性
- 角色-权限关联的事务性要求

#### ✅ **复杂业务规则**
- 密码策略(强度、历史、过期)
- 登录控制(失败锁定、异地登录)
- 数据权限过滤(本部门/本人/自定义)
- 角色继承与权限合并计算

#### ✅ **清晰的领域边界**
可划分为独立的限界上下文:
1. **认证上下文** (Authentication BC)
2. **授权上下文** (Authorization BC)
3. **身份管理上下文** (Identity BC)
4. **组织架构上下文** (Organization BC)

#### ✅ **长期演进需求**
- 支持新的认证方式(人脸识别、指纹)
- 支持更复杂的权限模型(ABAC、PBAC)
- 多租户隔离需求

### 3.2 挑战与风险

| 挑战 | 影响 | 应对策略 |
|------|------|----------|
| 团队学习曲线 | 中 | 提供DDD培训、代码示例、Code Review |
| 重构工作量 | 高 | 渐进式重构，优先核心领域 |
| 性能损耗 | 低 | 合理使用缓存、优化仓储查询 |
| 过度设计风险 | 中 | 遵循YAGNI原则，按需建模 |
| 与Framework集成 | 中 | 适配器模式隔离框架依赖 |

### 3.3 结论

**✅ 推荐采用DDD重构**

**理由**:
1. Auth-Service是**核心域**，业务复杂度高，值得精细建模
2. 现有Function.md已明确定义业务能力，便于领域划分
3. 微服务架构天然支持DDD的限界上下文隔离
4. 长期维护成本可通过更清晰的代码结构大幅降低

---

## 4. 架构设计方案

### 4.1 分层架构

采用**经典DDD四层架构** + **六边形架构(端口-适配器)**结合:

```
┌───────────────────────────────────────────────────────────┐
│                  Interfaces Layer (接口层)                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────┐  │
│  │  REST API       │  │  Feign Client   │  │  Event   │  │
│  │  (Controller)   │  │  (Anti-Corruption│  │ Listener │  │
│  └─────────────────┘  └─────────────────┘  └──────────┘  │
├───────────────────────────────────────────────────────────┤
│                Application Layer (应用层)                  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Application Services (应用服务)                     │  │
│  │  - 编排领域服务                                       │  │
│  │  - 事务管理                                          │  │
│  │  - DTO转换                                           │  │
│  │  - 权限校验                                          │  │
│  └─────────────────────────────────────────────────────┘  │
├───────────────────────────────────────────────────────────┤
│                  Domain Layer (领域层)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Entities   │  │  Value       │  │  Domain      │   │
│  │  (聚合根)     │  │  Objects     │  │  Services    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Repository  │  │  Domain      │  │  Factories   │   │
│  │  Interfaces  │  │  Events      │  │              │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
├───────────────────────────────────────────────────────────┤
│              Infrastructure Layer (基础设施层)             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │  Repository │  │  Cache      │  │  External       │  │
│  │  Impl       │  │  (Redis)    │  │  Services       │  │
│  │  (MyBatis)  │  │             │  │  (Member/OAuth) │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└───────────────────────────────────────────────────────────┘
```

### 4.2 限界上下文划分 (MVP版本)

```
┌─────────────────────────────────────────────────────────┐
│              Auth Service (认证授权服务)                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Authentication Context (认证上下文)              │  │
│  │  - 登录/登出 (无Session，纯JWT)                   │  │
│  │  - 多认证方式                                     │  │
│  │  │  - OAuth2认证 (GitHub/Google)                │  │
│  │  │  - 密码认证                                   │  │
│  │  │  - 微信开放平台                               │  │
│  │  - JWT Token管理 (Access + Refresh)              │  │
│  │  - Token黑名单 (登出/撤销)                        │  │
│  └──────────────────────────────────────────────────┘  │
│                        ▲                                │
│                        │ 依赖                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Authorization Context (授权上下文)               │  │
│  │  - 权限验证 (基于Spring Security)                 │  │
│  │  - 角色管理 (RBAC)                                │  │
│  │  - 权限资源管理 (API/菜单/按钮)                    │  │
│  │  - 权限缓存策略 (Redis)                           │  │
│  │  - 数据权限预留接口 (暂不实现)                    │  │
│  └──────────────────────────────────────────────────┘  │
│                        ▲                                │
│                        │ 依赖                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Identity Context (身份管理上下文)                 │  │
│  │  - 抽象UserPrincipal (支持多用户域)               │  │
│  │  │  - MemberPrincipal (会员)                     │  │
│  │  │  - AdminPrincipal (后台用户-预留)             │  │
│  │  - 密码策略                                       │  │
│  │  - 账户安全 (锁定/解锁)                           │  │
│  │  - 登录失败追踪                                   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Organization Context (组织架构上下文) - 预留     │  │
│  │  - 扩展接口定义                                   │  │
│  │  - 暂不实现具体功能                               │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Shared Kernel (共享内核)                         │  │
│  │  - UserPrincipal接口                              │  │
│  │  - 时间戳值对象 (UnixTimestamp)                   │  │
│  │  - 审计日志                                       │  │
│  │  - 通用异常                                       │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              Gateway (网关层)                            │
├─────────────────────────────────────────────────────────┤
│  - JWT Token验证 (本地解析)                             │
│  - 权限预检查 (缓存)                                     │
│  - 限流控制                                              │
│  - 调用auth-service刷新缓存                              │
└─────────────────────────────────────────────────────────┘
```

**上下文关系**:
- **Shared Kernel**: 被所有上下文共享
- **Customer-Supplier**: Authorization → Identity → Organization
- **Anti-Corruption Layer**: 与member-facade、framework集成

---

## 5. 领域模型设计

### 5.1 认证上下文 (Authentication Context)

#### 核心聚合

##### 1️⃣ **JwtToken 聚合** (无Session设计 + 自动续期)
```java
// 聚合根 - JWT Token管理
public class JwtToken {
    private TokenId tokenId;
    private UserPrincipal principal;         // 抽象用户主体
    private TokenType type;                  // ACCESS, REFRESH
    private String rawToken;
    private Set<String> authorities;         // 权限列表
    private LoginContext loginContext;       // 登录上下文(值对象)
    private Long issuedAt;                   // 签发时间戳
    private Long expiresAt;                  // 过期时间戳
    private Long lastRefreshedAt;            // 最后刷新时间戳 (用于滑动窗口)
    private boolean revoked;                 // 是否已撤销
    
    // 不变量
    private static final long REFRESH_WINDOW = 7 * 24 * 3600; // 7天内刷新可续期
    
    // 领域行为
    public boolean isExpired(Long currentTimestamp) {
        return currentTimestamp > expiresAt;
    }
    
    public void revoke() {
        this.revoked = true;
        registerEvent(new TokenRevokedEvent(this.tokenId, this.principal));
    }
    
    public TokenPair refresh(Long currentTimestamp) {
        if (this.type != TokenType.REFRESH) {
            throw new InvalidTokenOperationException("只有RefreshToken可以刷新");
        }
        
        // 生成新的AccessToken
        JwtToken newAccessToken = createAccessToken(this.principal, currentTimestamp);
        
        // 滑动窗口续期：如果在7天内刷新，RefreshToken也续期
        if (shouldRenewRefreshToken(currentTimestamp)) {
            JwtToken newRefreshToken = createRefreshToken(this.principal, currentTimestamp);
            return new TokenPair(newAccessToken, newRefreshToken);
        }
        
        return new TokenPair(newAccessToken, this);
    }
    
    private boolean shouldRenewRefreshToken(Long currentTimestamp) {
        long timeSinceLastRefresh = currentTimestamp - this.lastRefreshedAt;
        return timeSinceLastRefresh < REFRESH_WINDOW;
    }
}

// 值对象
public record LoginContext(
    LoginMethod method,        // PASSWORD, VERIFICATION_CODE, OAUTH2, WECHAT_SCAN
    String provider,           // github, google, wechat-open (OAuth2专用)
    IpAddress ipAddress,
    String userAgent,
    DeviceInfo deviceInfo,     // 设备信息
    Long loginTimestamp
) {}

public enum LoginMethod {
    WECHAT_MP,
    SMS
}
```

##### 2️⃣ **TokenBlacklist 聚合** (Token黑名单)
```java
// 聚合根 - 用于登出和Token撤销
public class TokenBlacklist {
    private TokenId tokenId;
    private String tokenJti;              // JWT ID
    private UserPrincipal principal;
    private Long blacklistedAt;           // 加入黑名单时间戳
    private Long expiresAt;               // 原Token过期时间戳
    private BlacklistReason reason;       // 黑名单原因
    
    // 领域行为
    public boolean isExpired(Long currentTimestamp) {
        return currentTimestamp > expiresAt;
    }
    
    public static TokenBlacklist fromToken(JwtToken token, BlacklistReason reason) {
        return new TokenBlacklist(
            token.getTokenId(),
            token.getJti(),
            token.getPrincipal(),
            System.currentTimeMillis() / 1000,
            token.getExpiresAt(),
            reason
        );
    }
}

public enum BlacklistReason {
    LOGOUT,              // 用户主动登出
    FORCED_LOGOUT,       // 强制登出
    PASSWORD_CHANGED,    // 密码修改
    PERMISSION_CHANGED,  // 权限变更
    SECURITY_CONCERN     // 安全原因
}
```

#### 领域服务

```java
public interface AuthenticationDomainService {
    // 密码认证
    AuthenticationResult authenticateWithPassword(
        String username,
        String password, 
        UserDomain userDomain  // MEMBER, ADMIN
    );
    
    // OAuth2认证
    AuthenticationResult authenticateWithOAuth2(
        OAuth2Code code, 
        OAuth2Provider provider,
        UserDomain userDomain
    );
    
    // 微信开放平台认证
    AuthenticationResult authenticateWithWechatOpen(String code);
    
    // JWT Token生成
    TokenPair generateTokenPair(UserPrincipal principal, LoginContext context);
    
    // Token刷新
    JwtToken refreshAccessToken(String refreshToken);
    
    // Token验证 (Gateway调用)
    Optional<UserPrincipal> validateToken(String token);
}

// 认证结果
public record AuthenticationResult(
    UserPrincipal principal,
    TokenPair tokenPair,
    Long authenticatedAt  // 时间戳
) {}

public record TokenPair(
    String accessToken,
    String refreshToken,
    Long accessTokenExpiresAt,   // 时间戳
    Long refreshTokenExpiresAt   // 时间戳
) {}
```

#### 仓储接口

```java
// Token黑名单仓储 (Redis实现)
public interface TokenBlacklistRepository {
    void add(TokenBlacklist blacklist);
    boolean isBlacklisted(String tokenJti);
    void removeExpired(Long currentTimestamp);
}

// RefreshToken仓储 (Redis实现，记录已签发的RefreshToken)
public interface RefreshTokenRepository {
    void save(String tokenJti, UserPrincipal principal, Long expiresAt);
    Optional<UserPrincipal> findByJti(String tokenJti);
    void revokeByPrincipal(UserPrincipal principal);
    void revokeByJti(String tokenJti);
}
```

---

### 5.2 授权上下文 (Authorization Context)

#### 核心聚合

##### 1️⃣ **Role 聚合**
```java
// 聚合根
public class Role {
    private RoleId roleId;
    private RoleName name;
    private RoleType type;  // SYSTEM, CUSTOM
    private RoleStatus status;
    private Set<Permission> permissions;  // 聚合内实体
    private DataScope dataScope;          // 数据权限范围
    
    // 不变量
    private void validatePermissions() {
        if (type == RoleType.SYSTEM && permissions.isEmpty()) {
            throw new DomainException("系统角色必须包含权限");
        }
    }
    
    // 领域行为
    public void grantPermission(Permission permission) { 
        permissions.add(permission);
        // 发布领域事件
        registerEvent(new RolePermissionGrantedEvent(this.roleId, permission));
    }
    
    public void revokePermission(PermissionId permissionId) { /* ... */ }
    public boolean hasPermission(String permissionCode) { /* ... */ }
}

// 聚合内实体
public class Permission {
    private PermissionId permissionId;
    private String code;              // "user:create"
    private PermissionResource resource;  // URL/MENU/BUTTON
    private String description;
}
```

##### 2️⃣ **User-Role Assignment 聚合**
```java
public class UserRoleAssignment {
    private AssignmentId assignmentId;
    private UserId userId;
    private Set<RoleId> roleIds;
    private TemporaryPermission temporaryPermission;  // 临时权限
    
    // 领域行为
    public void assignRole(RoleId roleId) { /* ... */ }
    public void grantTemporaryPermission(Permission permission, Duration duration) { /* ... */ }
    public Set<Permission> getAllEffectivePermissions(RoleRepository roleRepo) {
        // 聚合角色权限 + 临时权限
    }
}
```

#### 领域服务

```java
public interface PermissionEvaluationService {
    // 权限计算
    boolean hasPermission(UserId userId, String permissionCode);
    
    // 数据权限过滤
    DataFilter calculateDataFilter(UserId userId, DataScope scope);
    
    // 权限继承计算
    Set<Permission> calculateInheritedPermissions(Set<RoleId> roleIds);
}
```

#### 仓储接口

```java
public interface RoleRepository {
    void save(Role role);
    Optional<Role> findById(RoleId roleId);
    List<Role> findByIds(Set<RoleId> roleIds);
    List<Role> findByType(RoleType type);
}

public interface UserRoleAssignmentRepository {
    void save(UserRoleAssignment assignment);
    Optional<UserRoleAssignment> findByUserId(UserId userId);
}

public interface PermissionRepository {
    List<Permission> findAll();
    List<Permission> findByResource(PermissionResource resource);
}
```

---

### 5.3 身份管理上下文 (Identity Context)

#### 核心聚合

##### 1️⃣ **UserPrincipal 接口** (抽象用户主体)
```java
// 共享内核 - 抽象用户接口
public interface UserPrincipal {
    String getUserId();              // 用户唯一标识
    UserDomain getUserDomain();      // 用户域 (MEMBER, ADMIN)
    String getUsername();            // 用户名
    AccountStatus getStatus();       // 账户状态
    Set<String> getAuthorities();    // 权限集合
    
    // 用于JWT Payload
    Map<String, Object> toClaims();
    
    // 从JWT Claims重建
    static UserPrincipal fromClaims(Map<String, Object> claims);
}

public enum UserDomain {
    MEMBER,   // 会员域
    ADMIN     // 后台用户域(预留)
}
```

##### 2️⃣ **MemberPrincipal 聚合** (会员用户主体)
```java
// 聚合根 - 会员用户
public class MemberPrincipal implements UserPrincipal {
    private MemberId memberId;
    private Username username;
    private Email email;
    private PhoneNumber phone;
    private HashedPassword password;
    private AccountStatus status;
    private SecurityPolicy securityPolicy;     // 值对象
    private LoginAttemptTracker loginAttempts; // 聚合内实体
    private Long createdAt;                    // 时间戳
    private Long updatedAt;                    // 时间戳
    
    // 不变量
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    
    // 领域行为
    public void recordLoginAttempt(boolean success, IpAddress ip, Long timestamp) {
        loginAttempts.record(success, timestamp);
        if (loginAttempts.exceedsMaxAttempts(MAX_LOGIN_ATTEMPTS)) {
            this.lock(timestamp);
            registerEvent(new AccountLockedEvent(this.memberId, timestamp));
        }
    }
    
    public void changePassword(Password newPassword, PasswordPolicy policy, Long timestamp) {
        policy.validate(newPassword);
        if (securityPolicy.isInPasswordHistory(newPassword)) {
            throw new PasswordReusedException();
        }
        this.password = HashedPassword.from(newPassword);
        securityPolicy.addToPasswordHistory(newPassword);
        this.updatedAt = timestamp;
        registerEvent(new PasswordChangedEvent(this.memberId, timestamp));
    }
    
    public void lock(Long timestamp) { 
        status = AccountStatus.LOCKED;
        this.updatedAt = timestamp;
    }
    
    public void unlock(Long timestamp) { 
        status = AccountStatus.ACTIVE;
        loginAttempts.reset();
        this.updatedAt = timestamp;
    }
    
    @Override
    public String getUserId() { return memberId.value(); }
    
    @Override
    public UserDomain getUserDomain() { return UserDomain.MEMBER; }
    
    @Override
    public Map<String, Object> toClaims() {
        return Map.of(
            "userId", memberId.value(),
            "userDomain", "MEMBER",
            "username", username.value(),
            "authorities", getAuthorities()
        );
    }
}

// 值对象
public record SecurityPolicy(
    List<String> passwordHistory,
    Long passwordExpiresAt,        // 时间戳
    Set<String> ipWhitelist
) {
    public boolean isInPasswordHistory(Password password) { /* ... */ }
    public void addToPasswordHistory(Password password) { /* ... */ }
}
```

##### 3️⃣ **AdminPrincipal 聚合** (预留，未来实现)
```java
// 预留接口
public interface AdminPrincipal extends UserPrincipal {
    // 后台用户特有属性
    Set<Long> getDepartmentIds();  // 所属部门
    Long getPositionId();          // 职位
}
```

#### 领域服务

```java
public interface PasswordPolicyService {
    void validate(Password password);
    int calculateStrength(Password password);
}

public interface AccountSecurityService {
    void detectSuspiciousActivity(UserId userId, LoginActivity activity);
    void sendAnomalyAlert(UserId userId, AnomalyType type);
}
```

#### 仓储接口

```java
// 会员主体仓储
public interface MemberPrincipalRepository {
    void save(MemberPrincipal member);
    Optional<MemberPrincipal> findById(MemberId memberId);
    Optional<MemberPrincipal> findByUsername(Username username);
    Optional<MemberPrincipal> findByEmail(Email email);
    Optional<MemberPrincipal> findByPhone(PhoneNumber phone);
    List<MemberPrincipal> findByStatus(AccountStatus status);
}

// UserPrincipal工厂接口 (用于多用户域统一查询)
public interface UserPrincipalRepository {
    Optional<UserPrincipal> findByUsernameAndDomain(String username, UserDomain domain);
    Optional<UserPrincipal> findByIdAndDomain(String userId, UserDomain domain);
}
```

---

### 5.4 组织架构上下文 (Organization Context) - 预留扩展

> **设计原则**: 当前不实现具体功能，但预留扩展接口，确保未来Admin域引入部门功能时无需大规模重构

#### 扩展接口设计

```java
// 组织架构扩展接口 (Shared Kernel)
public interface OrganizationalEntity {
    Set<Long> getOrganizationIds();  // 所属组织单元ID列表
    Long getPrimaryOrganizationId(); // 主组织单元ID
}

// AdminPrincipal实现该接口
public interface AdminPrincipal extends UserPrincipal, OrganizationalEntity {
    @Override
    default Set<Long> getOrganizationIds() {
        return getDepartmentIds();
    }
}

// 数据权限扩展接口
public interface DataScopeProvider {
    /**
     * 计算用户可访问的数据范围
     * @return 组织单元ID集合，空集合表示无权限，null表示全部数据
     */
    Set<Long> calculateDataScope(UserPrincipal principal);
}

// 默认实现 (当前返回null，表示无组织限制)
public class DefaultDataScopeProvider implements DataScopeProvider {
    @Override
    public Set<Long> calculateDataScope(UserPrincipal principal) {
}
```

---

### 5.5 共享内核 (Shared Kernel)

#### 通用值对象

```java
// 用户ID
public record UserId(Long value) {
    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid UserId");
        }
    }
}

// 审计信息
public record AuditInfo(
    UserId createdBy,
    Instant createdAt,
    UserId updatedBy,
    Instant updatedAt
) {}

// IP地址
public record IpAddress(String value) {
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    public IpAddress {
        if (!IP_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid IP address");
        }
    }
}
```

#### 领域事件

```java
public interface DomainEvent {
    EventId eventId();
    Instant occurredOn();
}

public record AccountLockedEvent(
    EventId eventId,
    UserId userId,
    Instant occurredOn
) implements DomainEvent {}

public record RolePermissionGrantedEvent(
    EventId eventId,
    RoleId roleId,
    Permission permission,
    Instant occurredOn
) implements DomainEvent {}
```

---

## 6. 技术栈选型

### 6.1 核心框架

| 层级 | 技术选型 | 版本 | 理由 |
|------|---------|------|------|
| 应用框架 | Spring Boot | 3.2+ | 现有技术栈，成熟稳定 |
| 安全框架 | Spring Security | 6.2+ | ✅ 替换自研框架 |
| JWT库 | jjwt | 0.12+ | 稳定的JWT实现 |
| 持久化 | MyBatis Plus | 3.5+ | 现有技术栈，灵活ORM |
| 缓存 | Redis | 7.0+ | ✅ Token黑名单、权限缓存 (无Session) |
| 服务发现 | Nacos | 2.3+ | 现有基础设施 |
| 远程调用 | OpenFeign | 4.x | ✅ 仅Gateway→auth-service |

### 6.2 DDD支撑库

| 功能 | 技术选型 | 理由 |
|------|---------|------|
| 领域事件 | Spring Event | 轻量级，足够用 |
| 规约模式 | 自实现 | 简单场景，无需引入库 |
| 值对象 | Java Record | Java 14+原生支持 |
| 不可变集合 | Guava ImmutableList | 保护聚合内部状态 |

### 6.3 开发工具

| 类别 | 工具 | 用途 |
|------|------|------|
| 代码生成 | Lombok | 减少样板代码 |
| 校验 | Jakarta Validation | DTO/参数校验 |
| 映射 | MapStruct | DTO-Entity转换 |
| 测试 | JUnit 5 + Mockito | 单元测试 |
| 测试 | Testcontainers | 集成测试(Redis/MySQL) |

---

## 7. 目录结构设计

### 7.1 整体结构

```
auth-service/
├── pom.xml
├── DDD-REFACTORING-SPEC.md
├── src/
│   ├── main/
│   │   ├── java/com/pot/auth/
│   │   │   ├── AuthServiceApplication.java
│   │   │   │
│   │   │   ├── interfaces/              # 接口层
│   │   │   │   ├── rest/                # REST API
│   │   │   │   │   ├── authentication/
│   │   │   │   │   │   ├── AuthenticationController.java
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   │   │   └── OAuth2CallbackRequest.java
│   │   │   │   │   │   └── assembler/
│   │   │   │   │   │       └── AuthenticationDtoAssembler.java
│   │   │   │   │   ├── authorization/
│   │   │   │   │   │   ├── RoleController.java
│   │   │   │   │   │   ├── PermissionController.java
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── identity/
│   │   │   │   │   │   ├── UserAccountController.java
│   │   │   │   │   │   └── dto/
│   │   │   │   │   └── organization/
│   │   │   │   │       ├── DepartmentController.java
│   │   │   │   │       └── dto/
│   │   │   │   │
│   │   │   │   ├── facade/              # 对外Feign接口实现
│   │   │   │   │   └── AuthFacadeImpl.java
│   │   │   │   │
│   │   │   │   ├── event/               # 事件监听器
│   │   │   │   │   └── listener/
│   │   │   │   │       └── AccountEventListener.java
│   │   │   │   │
│   │   │   │   └── schedule/            # 定时任务
│   │   │   │       └── SessionCleanupTask.java
│   │   │   │
│   │   │   ├── application/             # 应用层
│   │   │   │   ├── service/             # 应用服务
│   │   │   │   │   ├── authentication/
│   │   │   │   │   │   ├── LoginApplicationService.java
│   │   │   │   │   │   ├── LogoutApplicationService.java
│   │   │   │   │   │   └── OAuth2ApplicationService.java
│   │   │   │   │   ├── authorization/
│   │   │   │   │   │   ├── RoleManagementApplicationService.java
│   │   │   │   │   │   ├── PermissionManagementApplicationService.java
│   │   │   │   │   │   └── PermissionCheckApplicationService.java
│   │   │   │   │   ├── identity/
│   │   │   │   │   │   ├── UserAccountManagementApplicationService.java
│   │   │   │   │   │   └── PasswordManagementApplicationService.java
│   │   │   │   │   └── organization/
│   │   │   │   │       ├── DepartmentManagementApplicationService.java
│   │   │   │   │       └── PositionManagementApplicationService.java
│   │   │   │   │
│   │   │   │   ├── assembler/           # DTO组装器
│   │   │   │   │   └── UserAccountAssembler.java
│   │   │   │   │
│   │   │   │   └── command/             # 命令对象
│   │   │   │       ├── CreateUserCommand.java
│   │   │   │       ├── AssignRoleCommand.java
│   │   │   │       └── ChangePasswordCommand.java
│   │   │   │
│   │   │   ├── domain/                  # 领域层 ⭐核心
│   │   │   │   ├── authentication/      # 认证上下文
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── aggregate/
│   │   │   │   │   │   │   ├── AuthenticationSession.java
│   │   │   │   │   │   │   └── JwtToken.java
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   └── LoginAttempt.java
│   │   │   │   │   │   └── valueobject/
│   │   │   │   │   │       ├── SessionId.java
│   │   │   │   │   │       ├── LoginMethod.java
│   │   │   │   │   │       ├── DeviceFingerprint.java
│   │   │   │   │   │       └── OAuth2Code.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── AuthenticationDomainService.java
│   │   │   │   │   │   ├── JwtTokenService.java
│   │   │   │   │   │   └── OAuth2AuthenticationService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── AuthenticationSessionRepository.java
│   │   │   │   │   │   └── JwtTokenRepository.java
│   │   │   │   │   ├── event/
│   │   │   │   │   │   ├── UserAuthenticatedEvent.java
│   │   │   │   │   │   └── SessionExpiredEvent.java
│   │   │   │   │   └── exception/
│   │   │   │   │       ├── InvalidCredentialsException.java
│   │   │   │   │       └── SessionExpiredException.java
│   │   │   │   │
│   │   │   │   ├── authorization/       # 授权上下文
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── aggregate/
│   │   │   │   │   │   │   ├── Role.java
│   │   │   │   │   │   │   └── UserRoleAssignment.java
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   ├── Permission.java
│   │   │   │   │   │   │   └── TemporaryPermission.java
│   │   │   │   │   │   └── valueobject/
│   │   │   │   │   │       ├── RoleId.java
│   │   │   │   │   │       ├── PermissionId.java
│   │   │   │   │   │       ├── DataScope.java
│   │   │   │   │   │       └── PermissionResource.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── PermissionEvaluationService.java
│   │   │   │   │   │   └── DataScopeCalculationService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── RoleRepository.java
│   │   │   │   │   │   ├── PermissionRepository.java
│   │   │   │   │   │   └── UserRoleAssignmentRepository.java
│   │   │   │   │   └── event/
│   │   │   │   │       ├── RolePermissionGrantedEvent.java
│   │   │   │   │       └── UserRoleAssignedEvent.java
│   │   │   │   │
│   │   │   │   ├── identity/            # 身份管理上下文
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── aggregate/
│   │   │   │   │   │   │   └── UserAccount.java
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   └── LoginAttemptTracker.java
│   │   │   │   │   │   └── valueobject/
│   │   │   │   │   │       ├── UserId.java
│   │   │   │   │   │       ├── Username.java
│   │   │   │   │   │       ├── Email.java
│   │   │   │   │   │       ├── PhoneNumber.java
│   │   │   │   │   │       ├── HashedPassword.java
│   │   │   │   │   │       ├── UserProfile.java
│   │   │   │   │   │       └── SecurityPolicy.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── PasswordPolicyService.java
│   │   │   │   │   │   └── AccountSecurityService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── UserAccountRepository.java
│   │   │   │   │   └── event/
│   │   │   │   │       ├── AccountCreatedEvent.java
│   │   │   │   │       ├── AccountLockedEvent.java
│   │   │   │   │       └── PasswordChangedEvent.java
│   │   │   │   │
│   │   │   │   ├── organization/        # 组织架构上下文
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── aggregate/
│   │   │   │   │   │   │   ├── Department.java
│   │   │   │   │   │   │   └── Position.java
│   │   │   │   │   │   └── valueobject/
│   │   │   │   │   │       ├── DepartmentId.java
│   │   │   │   │   │       ├── PositionId.java
│   │   │   │   │   │       └── ContactInfo.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── DepartmentHierarchyService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── DepartmentRepository.java
│   │   │   │   │   │   └── PositionRepository.java
│   │   │   │   │   └── event/
│   │   │   │   │       └── UserAssignedToDepartmentEvent.java
│   │   │   │   │
│   │   │   │   └── shared/              # 共享内核
│   │   │   │       ├── valueobject/
│   │   │   │       │   ├── AuditInfo.java
│   │   │   │       │   ├── IpAddress.java
│   │   │   │       │   └── EventId.java
│   │   │   │       ├── event/
│   │   │   │       │   ├── DomainEvent.java
│   │   │   │       │   └── DomainEventPublisher.java
│   │   │   │       ├── exception/
│   │   │   │       │   ├── DomainException.java
│   │   │   │       │   └── BusinessRuleViolationException.java
│   │   │   │       └── specification/
│   │   │   │           └── Specification.java
│   │   │   │
│   │   │   └── infrastructure/          # 基础设施层
│   │   │       ├── persistence/         # 持久化实现
│   │   │       │   ├── mybatis/
│   │   │       │   │   ├── mapper/
│   │   │       │   │   │   ├── UserAccountMapper.java
│   │   │       │   │   │   ├── RoleMapper.java
│   │   │       │   │   │   └── DepartmentMapper.xml
│   │   │       │   │   ├── dataobject/  # PO对象
│   │   │       │   │   │   ├── UserAccountDO.java
│   │   │       │   │   │   ├── RoleDO.java
│   │   │       │   │   │   └── PermissionDO.java
│   │   │       │   │   └── converter/   # DO-Entity转换器
│   │   │       │   │       ├── UserAccountConverter.java
│   │   │       │   │       └── RoleConverter.java
│   │   │       │   └── repository/      # 仓储实现
│   │   │       │       ├── UserAccountRepositoryImpl.java
│   │   │       │       ├── RoleRepositoryImpl.java
│   │   │       │       └── DepartmentRepositoryImpl.java
│   │   │       │
│   │   │       ├── cache/               # 缓存实现
│   │   │       │   ├── RedisCacheManager.java
│   │   │       │   ├── PermissionCacheService.java
│   │   │       │   └── SessionCacheService.java
│   │   │       │
│   │   │       ├── external/            # 外部服务适配器
│   │   │       │   ├── member/
│   │   │       │   │   └── MemberServiceAdapter.java
│   │   │       │   ├── oauth2/
│   │   │       │   │   ├── GitHubOAuth2Provider.java
│   │   │       │   │   ├── GoogleOAuth2Provider.java
│   │   │       │   │   └── WechatOAuth2Provider.java
│   │   │       │   └── sms/
│   │   │       │       └── SmsServiceAdapter.java
│   │   │       │
│   │   │       ├── security/            # 安全框架集成
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   ├── SecurityContextHelper.java
│   │   │       │   └── PermissionInterceptor.java
│   │   │       │
│   │   │       └── config/              # 基础设施配置
│   │   │           ├── MyBatisConfig.java
│   │   │           ├── RedisConfig.java
│   │   │           └── FeignConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-security.yml
│   │       ├── mapper/                  # MyBatis XML
│   │       │   ├── UserAccountMapper.xml
│   │       │   ├── RoleMapper.xml
│   │       │   └── DepartmentMapper.xml
│   │       └── db/
│   │           └── migration/           # Flyway迁移脚本
│   │               ├── V1__create_user_account_table.sql
│   │               └── V2__create_role_permission_tables.sql
│   │
│   └── test/
│       └── java/com/pot/auth/
│           ├── domain/                  # 领域层单元测试
│           │   ├── authentication/
│           │   │   └── AuthenticationSessionTest.java
│           │   ├── authorization/
│           │   │   └── RoleTest.java
│           │   └── identity/
│           │       └── UserAccountTest.java
│           │
│           ├── application/             # 应用服务集成测试
│           │   └── LoginApplicationServiceTest.java
│           │
│           └── architecture/            # 架构测试
│               └── DddArchitectureTest.java
```

### 7.2 分层依赖规则

```
✅ 允许的依赖方向:
interfaces → application → domain
infrastructure → domain (仅实现仓储接口)

❌ 禁止的依赖:
domain → infrastructure
domain → application
domain → interfaces
```

---

## 8. 实施路线图 (MVP版本)

### Phase 1: 准备阶段 (1周)

**目标**: 团队准备 + 技术验证

| 任务 | 负责人 | 产出 |
|------|--------|------|
| DDD培训 | 架构师 | 培训材料、代码示例 |
| Spring Security集成验证 | Tech Leader | JWT + 无Session配置 |
| RSA密钥对生成 | DevOps | 公钥/私钥文件 |
| 数据库设计 | DBA | 新表结构设计、Flyway脚本 |
| Redis Pub/Sub验证 | Tech Leader | 黑名单同步POC |

**验收标准**:
- [ ] 全员理解聚合、值对象、领域服务概念
- [ ] Spring Security 6 + JWT配置成功
- [ ] RSA密钥对生成并配置
- [ ] 数据库迁移脚本就绪

---

### Phase 2: 共享内核 + 身份管理上下文 (2-3周)

**优先级**: 🔴 高 (基础上下文)

**任务列表**:

#### 2.1 共享内核实现
- [ ] `UserPrincipal`接口定义
- [ ] `UserDomain`枚举
- [ ] `UnixTimestamp`值对象
- [ ] `AuditInfo`值对象
- [ ] `DomainEvent`接口
- [ ] `DomainException`体系

#### 2.2 身份管理领域建模

**用户聚合**:
- [ ] `MemberPrincipal`聚合根
- [ ] 值对象实现
  - [ ] `MemberId`、`Username`、`Email`、`PhoneNumber`
  - [ ] `HashedPassword` (BCrypt)
  - [ ] `SecurityPolicy`
- [ ] `LoginAttemptTracker`实体
- [ ] `PasswordPolicyService`领域服务
- [ ] 单元测试 (>80%覆盖率)

**注册聚合** (新增):
- [ ] `UserRegistration`聚合根
- [ ] `RegistrationId`、`RegistrationMethod`、`RegistrationStatus`
- [ ] `RegistrationData`值对象
- [ ] `UserRegistrationService`领域服务
- [ ] 注册流程单元测试

#### 2.3 基础设施实现
- [ ] MyBatis Mapper编写
  - [ ] `MemberPrincipalMapper`
  - [ ] `UserRegistrationMapper`
- [ ] 仓储实现
  - [ ] `MemberPrincipalRepositoryImpl`
  - [ ] `UserPrincipalRepositoryImpl` (工厂模式)
  - [ ] `UserRegistrationRepositoryImpl`
- [ ] Flyway迁移脚本
  - [ ] `V1__create_member_principal_table.sql`
  - [ ] `V2__create_login_attempt_table.sql`
  - [ ] `V3__create_user_registration_table.sql`

#### 2.4 应用层开发

**用户管理服务**:
- [ ] `MemberManagementApplicationService`
- [ ] `PasswordManagementApplicationService`

**注册服务** (新增):
- [ ] `UserRegistrationApplicationService`
  - [ ] 密码注册流程
  - [ ] 验证码注册流程
  - [ ] 邮箱验证流程
  - [ ] 手机号验证流程
- [ ] DTO定义 (MapStruct)
  - [ ] `RegisterRequest`
  - [ ] `RegisterResponse`
  - [ ] `VerifyEmailRequest`
- [ ] 集成测试 (Testcontainers)

#### 2.5 接口层暴露

**会员管理接口**:
- [ ] `MemberController`

**注册接口** (新增):
- [ ] `RegistrationController`
  - [ ] `POST /auth/register/password` (密码注册)
  - [ ] `POST /auth/register/code` (验证码注册)
  - [ ] `POST /auth/register/verify-email` (验证邮箱)
  - [ ] `POST /auth/register/verify-phone` (验证手机号)
  - [ ] `POST /auth/register/resend-verification` (重发验证邮件/短信)
- [ ] REST API文档 (Swagger)

**验收标准**:
- [ ] ✅ 会员密码注册功能正常 (用户名/邮箱/手机号)
- [ ] ✅ 验证码注册功能正常 (邮箱/手机号)
- [ ] ✅ 注册时检查用户名/邮箱/手机号唯一性
- [ ] ✅ 邮箱验证功能正常 (发送验证链接/验证码)
- [ ] ✅ 手机号验证功能正常 (发送短信验证码)
- [ ] ✅ 注册后账户状态为PENDING_VERIFICATION
- [ ] ✅ 验证后账户状态变为ACTIVE
- [ ] ✅ 验证链接24小时过期
- [ ] ✅ 密码策略验证生效 (最小长度、复杂度)
- [ ] ✅ 账户锁定/解锁功能正常
- [ ] ✅ 登录失败追踪正常
- [ ] ✅ 所有测试通过

---

### Phase 3: 认证上下文 (3-4周)

**优先级**: 🔴 高

**任务列表**:

#### 3.1 JWT Token管理 (含自动续期)
- [ ] `JwtToken`聚合根
  - [ ] 滑动窗口续期逻辑
  - [ ] `lastRefreshedAt`字段
- [ ] `TokenBlacklist`聚合根
- [ ] `LoginContext`值对象 (含设备信息)
- [ ] `DeviceInfo`值对象
- [ ] `JwtTokenService`领域服务
- [ ] Spring Security集成
  - [ ] `JwtTokenProvider` (签发/验证)
  - [ ] `JwtAuthenticationFilter`

#### 3.2 多种认证方式实现
- [ ] 1. 密码认证
  - [ ] 支持用户名/邮箱/手机号登录
  - [ ] `PasswordAuthenticationService`
- [ ] 2. 验证码认证
  - [ ] `VerificationCode`聚合根
  - [ ] `VerificationCodeService`领域服务
  - [ ] 邮箱验证码发送 (集成framework-starter-touch)
  - [ ] 短信验证码发送 (集成三方短信服务)
  - [ ] **验证码注册后自动登录**
- [ ] 3. OAuth2认证
  - [ ] `OAuth2AuthenticationService`
  - [ ] GitHub Provider
  - [ ] Google Provider
  - [ ] 预留扩展接口 (`OAuth2ProviderFactory`)
  - [ ] **OAuth2注册后自动登录**
- [ ] 4. 微信扫码认证
  - [ ] `WechatScanAuthenticationService`
  - [ ] 微信开放平台集成
  - [ ] **微信扫码注册后自动登录**

#### 3.3 设备管理
- [ ] `UserDevice`聚合根
- [ ] `DeviceManagementService`领域服务
- [ ] 设备登录记录
- [ ] 异地登录检测
- [ ] 踢出设备功能

#### 3.4 Token管理基础设施
- [ ] `TokenBlacklistRepository` (Redis实现)
- [ ] `RefreshTokenRepository` (Redis实现，含deviceId)
- [ ] `UserDeviceRepository` (MySQL实现)
- [ ] `VerificationCodeRepository` (Redis实现，5分钟TTL)
- [ ] Redis Pub/Sub配置
  - [ ] 黑名单变更事件发布
  - [ ] 设备踢出事件发布
  - [ ] Gateway订阅配置示例

- [ ] `LogoutApplicationService`
- [ ] `TokenRefreshApplicationService`
- [ ] `OAuth2ApplicationService`

#### 3.5 REST API
- [ ] `AuthenticationController`
  - [ ] `POST /auth/login` (密码登录)
  - [ ] `POST /auth/logout` (登出)
  - [ ] `POST /auth/refresh` (刷新Token)
  - [ ] `POST /auth/validate` (Gateway调用)
- [ ] `OAuth2Controller`
  - [ ] `GET /oauth2/authorize/{provider}`
  - [ ] `GET /oauth2/callback/{provider}`

**验收标准**:
- [ ] 密码登录成功返回AccessToken + RefreshToken
- [ ] JWT Token包含正确的Claims (userId, userDomain, authorities)
- [ ] RefreshToken可刷新AccessToken
- [ ] 登出后Token加入黑名单
- [ ] OAuth2登录流程通畅 (GitHub/Google)
- [ ] 微信开放平台登录正常
- [ ] 黑名单同步到Redis
- [ ] 所有测试通过

---

### Phase 4: 授权上下文 (3周)

**优先级**: 🔴 高

**任务列表**:

#### 4.1 权限模型实现
- [ ] `Role`聚合根
- [ ] `Permission`实体
- [ ] `UserRoleAssignment`聚合
- [ ] `TemporaryPermission`实体 (临时权限)
- [ ] 值对象
  - [ ] `RoleId`、`PermissionId`
  - [ ] `PermissionResource` (API/MENU/BUTTON)

#### 4.2 权限计算
- [ ] `PermissionEvaluationService`
- [ ] `DataScopeCalculationService` (预留接口)
- [ ] 权限继承计算
- [ ] 权限合并逻辑

#### 4.3 Spring Security集成
- [ ] 自定义`PermissionEvaluator`
- [ ] `@PreAuthorize("hasPermission('user:create')")`支持
- [ ] 方法级权限拦截
- [ ] URL级权限拦截

#### 4.4 权限缓存
- [ ] `PermissionCacheService` (Redis)
- [ ] 缓存Key设计
  - [ ] `auth:permission:user:{userId}`
  - [ ] `auth:permission:role:{roleId}`
- [ ] 缓存失效策略
  - [ ] 监听权限变更事件
  - [ ] 自动刷新缓存

#### 4.5 基础设施实现
- [ ] `RoleRepositoryImpl`
- [ ] `PermissionRepositoryImpl`
- [ ] `UserRoleAssignmentRepositoryImpl`
- [ ] Flyway迁移脚本
  - [ ] `V3__create_role_permission_tables.sql`

#### 4.6 应用层服务
- [ ] `RoleManagementApplicationService`
- [ ] `PermissionManagementApplicationService`
- [ ] `PermissionCheckApplicationService`

#### 4.7 REST API
- [ ] `RoleController`
  - [ ] 角色CRUD
  - [ ] 分配权限
- [ ] `PermissionController`
  - [ ] 权限资源管理
  - [ ] 权限树查询
- [ ] `UserRoleController`
  - [ ] 用户角色分配
  - [ ] 临时权限授予

**验收标准**:
- [ ] RBAC权限模型正常工作
- [ ] 角色-权限关联正确
- [ ] 用户-角色分配正常
- [ ] 权限检查准确 (URL/方法级)
- [ ] 权限缓存命中率>90%
- [ ] 权限变更后缓存自动刷新
- [ ] 临时权限过期自动失效
- [ ] 性能满足要求 (<100ms)

---

### Phase 5: Gateway集成 (1周)

**优先级**: 🔴 高

**任务列表**:

#### 5.1 Gateway JWT验证
- [ ] `JwtAuthenticationFilter`
- [ ] `JwtTokenValidator` (本地验证)
- [ ] RSA公钥配置

#### 5.2 Token黑名单同步
- [ ] `TokenBlacklistCache` (Caffeine + Redis)
- [ ] Redis Pub/Sub订阅
- [ ] 黑名单检查逻辑

#### 5.3 限流实现
- [ ] `RateLimitFilter`
- [ ] Redis令牌桶算法
- [ ] 用户级限流

#### 5.4 权限预检查
- [ ] `PermissionCache` (Gateway侧)
- [ ] 简单路径权限匹配
- [ ] 权限缓存失效订阅

#### 5.5 配置与测试
- [ ] Gateway配置文件
- [ ] 路由配置
- [ ] 白名单配置
- [ ] 集成测试

**验收标准**:
- [ ] Gateway可本地验证JWT Token
- [ ] 黑名单检查延迟<2ms
- [ ] 限流功能正常
- [ ] 权限预检查命中率>95%
- [ ] 无需调用Auth-Service即可完成认证鉴权

---

### Phase 6: 高级功能与优化 (1-2周)

**优先级**: 🟡 中

**任务列表**:

#### 6.1 监控运维
- [ ] 在线用户统计 (基于RefreshToken)
- [ ] 强制用户下线
- [ ] 操作日志记录 (AOP)
- [ ] 登录日志查询

#### 6.2 安全增强
- [ ] IP黑白名单
- [ ] 异地登录检测
- [ ] 可疑活动告警
- [ ] 密码过期提醒

#### 6.3 性能优化
- [ ] 数据库索引优化
- [ ] 缓存策略调优
- [ ] 慢查询优化
- [ ] 性能压测

#### 6.4 扩展性预留
- [ ] `OrganizationalEntity`接口定义
- [ ] `DataScopeProvider`接口实现
- [ ] `AdminPrincipal`预留实现

**验收标准**:
- [ ] 可查询在线用户列表
- [ ] 可强制用户下线
- [ ] IP黑名单生效
- [ ] QPS达到1000+
- [ ] P99延迟<200ms
- [ ] 缓存命中率>95%

---

### Phase 7: 测试与上线 (1周)

**任务列表**:

#### 7.1 全面测试
- [ ] 单元测试覆盖率>80%
- [ ] 集成测试覆盖核心流程
- [ ] 性能测试报告
- [ ] 安全测试 (OWASP Top 10)
- [ ] 压力测试 (JMeter)

#### 7.2 文档完善
- [ ] API文档 (Swagger)
- [ ] 部署文档
- [ ] 运维手册
- [ ] DDD架构图更新

#### 7.3 上线准备
- [ ] 生产环境配置
- [ ] 密钥生成与部署
- [ ] 监控告警配置
- [ ] 回滚预案

#### 7.4 灰度发布
- [ ] 金丝雀发布 (5% → 50% → 100%)
- [ ] 监控关键指标
- [ ] 用户反馈收集

**验收标准**:
- [ ] 所有测试通过
- [ ] 文档完整
- [ ] 性能达标
- [ ] 成功灰度发布
- [ ] 无严重bug

---

## 📅 时间线总结

| 阶段 | 工期 | 累计 | 关键产出 |
|------|------|------|----------|
| Phase 1: 准备 | 1周 | 1周 | DDD培训、Spring Security验证 |
| Phase 2: 身份管理 | 2周 | 3周 | MemberPrincipal聚合 |
| Phase 3: 认证 | 3-4周 | 6-7周 | 多种登录方式 + 设备管理 + Token自动续期 |
| Phase 4: 授权 | 3周 | 9-10周 | RBAC权限模型 |
| Phase 5: Gateway集成 | 1周 | 10-11周 | 网关认证鉴权 |
| Phase 6: 高级功能 | 1-2周 | 11-13周 | 监控、安全增强 |
| Phase 7: 测试上线 | 1周 | 12-14周 | 灰度发布 |

**总工期**: 约12-14周 (3-3.5个月)

---

## 9. 风险评估与应对

### 9.1 技术风险

| 风险项 | 等级 | 概率 | 影响 | 应对措施 |
|--------|------|------|------|----------|
| 团队DDD经验不足 | 🟡 中 | 高 | 中 | 1. 提供DDD培训<br>2. Code Review严格把关<br>3. 结对编程 |
| 性能下降 | 🟡 中 | 中 | 高 | 1. 性能测试先行<br>2. 缓存策略优化<br>3. 仓储查询优化 |
| 与Framework集成问题 | 🟢 低 | 中 | 中 | 1. 适配器模式隔离<br>2. 早期集成测试 |
| 数据库迁移失败 | 🟡 中 | 低 | 高 | 1. Flyway版本控制<br>2. 灰度迁移<br>3. 回滚脚本 |

### 9.2 业务风险

| 风险项 | 等级 | 概率 | 影响 | 应对措施 |
|--------|------|------|------|----------|
| 需求理解偏差 | 🟡 中 | 中 | 高 | 1. 事件风暴Workshop<br>2. 业务专家参与建模 |
| 功能遗漏 | 🟢 低 | 中 | 中 | 1. 功能清单checklist<br>2. UAT测试 |
| 上线后bug | 🟡 中 | 中 | 高 | 1. 充分测试<br>2. 灰度发布<br>3. 快速回滚 |

### 9.3 进度风险

| 风险项 | 等级 | 概率 | 影响 | 应对措施 |
|--------|------|------|------|----------|
| 工期延误 | 🟡 中 | 中 | 中 | 1. 每周进度review<br>2. 灵活调整优先级<br>3. 增加人力投入 |
| 资源不足 | 🟢 低 | 低 | 高 | 1. 提前资源规划<br>2. 外部支持 |

---

## 10. 验收标准

### 10.1 架构质量标准

| 维度 | 标准 | 验证方式 |
|------|------|----------|
| 分层依赖 | 严格遵守依赖规则 | ArchUnit测试 |
| 聚合完整性 | 每个聚合有明确边界 | Code Review |
| 领域逻辑内聚 | 业务规则在领域层 | 静态分析 |
| 无贫血模型 | 实体包含行为方法 | Code Review |

### 10.2 功能完整性标准

**核心功能checklist**:
- [ ] 用户名密码登录
- [ ] OAuth2登录(GitHub/Google)
- [ ] JWT Token生成与验证
- [ ] 用户管理(CRUD)
- [ ] 角色管理
- [ ] 权限分配
- [ ] 权限验证(URL/方法/按钮)
- [ ] 数据权限过滤
- [ ] 部门管理
- [ ] 密码策略
- [ ] 账户锁定
- [ ] 在线用户管理
- [ ] 操作日志

### 10.3 性能标准

| 指标 | 目标值 | 测试条件 |
|------|--------|----------|
| QPS | >1000 | 登录接口 |
| 权限检查延迟 | <50ms | P99 |
| 登录接口延迟 | <200ms | P99 |
| 缓存命中率 | >95% | 权限查询 |
| 并发用户数 | >5000 | 压力测试 |

### 10.4 代码质量标准

| 维度 | 标准 | 工具 |
|------|------|------|
| 单元测试覆盖率 | >80% | JaCoCo |
| 代码重复率 | <5% | SonarQube |
| 代码复杂度 | <10 | SonarQube |
| 代码规范 | 无违规 | Checkstyle |

---

## 11. 批准与确认

### 11.1 架构评审

**评审要点**:
1. ✅ 限界上下文划分是否合理
2. ✅ 聚合边界是否清晰
3. ✅ 领域模型是否符合业务语言
4. ✅ 技术选型是否合理
5. ✅ 实施路线是否可行

### 11.2 已确认架构决策 ✅

**用户已确认的架构选型**:
1. ✅ **Spring Security 6.x** - 作为安全框架，替换自研框架
2. ✅ **无Session设计** - 纯JWT Token，不依赖Session
3. ✅ **时间戳标准化** - 所有时间字段使用Unix时间戳(Long)
4. ✅ **多用户域支持** - Member/Admin分离，抽象UserPrincipal接口
5. ✅ **Gateway承担认证鉴权** - 本地JWT验证 + Redis Pub/Sub同步
6. ✅ **组织架构预留** - 当前不实现，仅定义扩展接口

### 11.3 待确认事项 (可选)

**补充确认问题**:
1. ⚠️ 是否需要支持**多因素认证**(MFA)？如TOTP、短信验证码
2. ⚠️ OAuth2是否需要支持更多提供商？(企业微信、钉钉、GitLab等)
3. ⚠️ 是否需要**审计日志持久化**？还是仅Redis缓存
4. ⚠️ RefreshToken是否需要**自动续期**？(滑动窗口机制)
5. ⚠️ 是否需要**设备管理**？(记录用户登录设备，支持踢出特定设备)

---

## 📌 请确认以上架构设计和技术规格

**下一步行动**:
- ✅ 如果认可此方案，请明确确认，我将进入实施阶段
- 🔄 如果需要调整，请指出具体修改点
- ❓ 如有疑问，请提出，我将详细解答

**确认后，我将**:
1. 开始创建详细的领域模型代码
2. 搭建DDD分层目录结构
3. 实现首个上下文(身份管理)的完整功能
4. 提供单元测试示例

---

**文档版本**: v1.0  
**最后更新**: 2025-11-09  
**状态**: 待批准 ⏳

