# Auth-Service DDD重构技术规格文档

> **版本**: v7.0 - 防腐层完整版  
> **日期**: 2025年11月10日  
> **状态**: ✅ 架构升级 - 增加基础设施防腐层  
> **核心原则**: Auth-Service是**无状态、无数据库**的纯认证授权服务  
> **架构升级**: 对所有外部依赖（Redis、Security框架、通知服务、外部服务）建立防腐层，确保核心领域不依赖具体实现

---

## 📋 目录

1. [需求理解总结](#1-需求理解总结)
2. [核心架构原则](#2-核心架构原则)
3. [系统架构设计](#3-系统架构设计)
4. [限界上下文划分](#4-限界上下文划分)
5. [领域模型设计](#5-领域模型设计)
6. [防腐层设计](#6-防腐层设计) ⭐⭐⭐
    - 6.0 防腐层架构总览
    - 6.1 缓存防腐层 (CachePort)
    - 6.2 用户模块防腐层 (UserModulePort)
    - 6.3 Token管理防腐层 (TokenManagementPort)
    - 6.4 通知防腐层 (NotificationPort)
    - 6.5 分布式锁防腐层 (DistributedLockPort)
    - 6.6 防腐层架构总结
    - 6.7 Feign Client定义
    - 6.8 防腐层设计总结
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

### 2.3 多用户域扩展性设计 ⭐

**设计目标**: Auth-Service作为**统一认证授权中心**，能够灵活接入多种用户域

#### 2.3.1 用户域抽象

```java
public enum UserDomain {
    MEMBER,      // 会员域 - C端用户
    ADMIN,       // 后台用户域 - B端员工
    MERCHANT,    // 商户域 - 商家用户 (未来)
    PARTNER,     // 合作伙伴域 - 渠道伙伴 (未来)
    AGENT        // 代理商域 - 代理用户 (未来)
    // 可以无限扩展...
}
```

#### 2.3.2 防腐层策略模式

```java
// 用户模块适配器接口 (统一抽象)
public interface UserModuleAdapter {
    // 标识当前适配器支持的用户域
    UserDomain supportedDomain();

    // 认证相关
    Optional<UserDTO> authenticateWithPassword(String identifier, String password);

    Optional<UserDTO> findById(String userId);

    // 注册相关
    String createUser(CreateUserRequest request);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // 权限查询
    Set<String> getPermissions(String userId);

    Set<RoleDTO> getRoles(String userId);

    // 设备管理
    List<DeviceDTO> getDevices(String userId);

    void recordDeviceLogin(String userId, DeviceInfo deviceInfo, IpAddress ip, String refreshToken);

    void kickDevice(String userId, String deviceId);

    // ... 其他方法
}

// Member域适配器实现
@Component
public class MemberModuleAdapter implements UserModuleAdapter {
    private final MemberServiceClient memberClient;

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.MEMBER;
    }

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        // 调用member-service
        return memberClient.authenticate(identifier, password);
    }

    // ... 其他方法实现
}

// Admin域适配器实现 (未来)
@Component
public class AdminModuleAdapter implements UserModuleAdapter {
    private final AdminServiceClient adminClient;

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.ADMIN;
    }

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        // 调用admin-service
        return adminClient.authenticate(identifier, password);
    }

    // ... 其他方法实现
}

// Merchant域适配器实现 (未来)
@Component
public class MerchantModuleAdapter implements UserModuleAdapter {
    private final MerchantServiceClient merchantClient;

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.MERCHANT;
    }

    // ... 实现细节
}
```

#### 2.3.3 适配器工厂

```java
// 适配器工厂 - 根据UserDomain动态获取对应的适配器
@Component
public class UserModuleAdapterFactory {

    private final Map<UserDomain, UserModuleAdapter> adapters;

    public UserModuleAdapterFactory(List<UserModuleAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        UserModuleAdapter::supportedDomain,
                        adapter -> adapter
                ));
    }

    public UserModuleAdapter getAdapter(UserDomain domain) {
        UserModuleAdapter adapter = adapters.get(domain);
        if (adapter == null) {
            throw new UnsupportedUserDomainException("不支持的用户域: " + domain);
        }
        return adapter;
    }

    public boolean supports(UserDomain domain) {
        return adapters.containsKey(domain);
    }

    public Set<UserDomain> getSupportedDomains() {
        return adapters.keySet();
    }
}
```

#### 2.3.4 应用层使用示例

```java

@Service
public class LoginApplicationService {

    private final UserModuleAdapterFactory adapterFactory;
    private final AuthenticationDomainService authenticationService;

    public LoginResponse login(LoginCommand command) {
        // 1. 根据userDomain获取对应的适配器
        UserModuleAdapter adapter = adapterFactory.getAdapter(command.userDomain());

        // 2. 使用适配器进行认证
        AuthenticationResult result = authenticationService.authenticateWithPassword(
                command.identifier(),
                command.password(),
                command.userDomain(),
                command.deviceInfo(),
                command.ipAddress(),
                adapter  // 传入对应域的适配器
        );

        // 3. 返回统一的认证结果
        return new LoginResponse(
                result.userId(),
                result.userDomain(),
                result.username(),
                result.tokenPair().accessToken(),
                result.tokenPair().refreshToken(),
                result.tokenPair().accessTokenExpiresAt(),
                result.tokenPair().refreshTokenExpiresAt()
        );
    }
}
```

#### 2.3.5 JWT Token中的域标识

```java
// JWT Token的Payload包含userDomain字段
{
        "jti":"token-uuid",
        "userId":"user-123",
        "userDomain":"MEMBER",  // ⭐ 用户域标识
        "username":"john_doe",
        "authorities":["user:read","user:write"],
        "iat":1699516800,
        "exp":1699520400
        }

// Gateway验证Token时可以识别用户来自哪个域
// Backend Service从Header获取userDomain
X-User-Id:user-123
X-User-Domain:MEMBER
X-Authorities:user:read,user:write
```

#### 2.3.6 扩展新用户域的步骤

**添加新用户域只需3步**:

```
1. 在UserDomain枚举中添加新域
   public enum UserDomain {
       MEMBER,
       ADMIN,
       MERCHANT  // ← 新增
   }

2. 创建对应的Feign Client
   @FeignClient(name = "merchant-service")
   public interface MerchantServiceClient {
       // 定义merchant-service的内部API
   }

3. 实现UserModuleAdapter
   @Component
   public class MerchantModuleAdapter implements UserModuleAdapter {
       private final MerchantServiceClient merchantClient;
       
       @Override
       public UserDomain supportedDomain() {
           return UserDomain.MERCHANT;
       }
       
       // 实现所有接口方法
   }

✅ 完成！Auth-Service自动支持新域
   - 不需要修改Auth-Service核心代码
   - 适配器工厂自动注册新适配器
   - 所有现有功能自动支持新域
```

#### 2.3.7 Redis缓存的域隔离

```java
// 权限缓存按域隔离
Key:auth:permissions:{userId}:{userDomain}

示例:
auth:permissions:user-123:MEMBER      → ["user:read","user:write"]
auth:permissions:admin-456:ADMIN      → ["admin:manage","system:config"]
auth:permissions:merchant-789:MERCHANT → ["shop:manage","product:edit"]

// RefreshToken也包含域信息
Key:auth:refresh:{jti}
Value:{
        "userId":"user-123",
        "userDomain":"MEMBER",
        "deviceId":"device-xxx",
        "issuedAt":1699516800,
        "expiresAt":1702108800
        }
```

#### 2.3.8 多域登录示例

```java
// 会员登录
POST /auth/login/

password {
    "identifier":"john_doe",
            "password":"xxx",
            "userDomain":"MEMBER"  // ← 指定域
}

// 后台用户登录
POST /auth/login/

password {
    "identifier":"admin001",
            "password":"xxx",
            "userDomain":"ADMIN"   // ← 指定域
}

// 商户登录 (未来)
POST /auth/login/

password {
    "identifier":"merchant001",
            "password":"xxx",
            "userDomain":"MERCHANT" // ← 指定域
}

// Auth-Service自动路由到对应的service
MEMBER    →member-service
ADMIN     →admin-service
MERCHANT  →merchant-service
```

#### 2.3.9 不同域的差异化处理

```java
// 某些域可能有特殊的认证逻辑
public class AdminModuleAdapter implements UserModuleAdapter {

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        // Admin域特殊逻辑: 需要MFA二次验证
        UserDTO user = adminClient.authenticate(identifier, password);

        if (user != null && !user.isMfaVerified()) {
            throw new MfaRequiredException("需要二次验证");
        }

        return Optional.of(user);
    }
}

// 某些域可能有特殊的权限结构
public class MerchantModuleAdapter implements UserModuleAdapter {

    @Override
    public Set<String> getPermissions(String userId) {
        // Merchant域特殊逻辑: 权限包含店铺ID
        Set<String> permissions = merchantClient.getPermissions(userId);

        // 添加店铺级别权限
        String shopId = merchantClient.getShopId(userId);
        permissions.add("shop:" + shopId + ":manage");

        return permissions;
    }
}
```

#### 2.3.10 扩展性优势总结

| 扩展场景    | 传统方式               | DDD + 适配器模式     |
|---------|--------------------|-----------------|
| 新增用户域   | 修改Auth-Service核心代码 | ✅ 只需添加Adapter   |
| 修改某域逻辑  | 影响所有域              | ✅ 只影响该域Adapter  |
| 不同域的差异化 | if-else堆砌          | ✅ 多态实现          |
| 测试      | 难以隔离               | ✅ 每个Adapter独立测试 |
| 维护性     | 耦合严重               | ✅ 高内聚低耦合        |

**扩展能力**:

- ✅ 支持无限多个用户域
- ✅ 每个域独立演进，互不影响
- ✅ 新增域无需修改Auth-Service核心代码
- ✅ 符合开闭原则 (对扩展开放，对修改关闭)

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
│  └────────────────────────────────────────���  │
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
           │ Feign调用         │ Feign调用
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

**值对象** (无持久化):

- `JwtToken` - JWT Token值对象
- `TokenBlacklist` - Token黑名单值对象
- `VerificationCode` - 验证码值对象

**领域服务**:

- `AuthenticationDomainService` - 认证逻辑
- `JwtTokenService` - Token生成与验证
- `VerificationCodeService` - 验证码管理

**数据存储**:

- ✅ Redis: Token黑名单、RefreshToken、验证码
- ❌ MySQL: 无

---

### 4.2 注册编排上下文 (Registration Orchestration Context)

**核心职责**: 编排用户注册流程

**领域服务**:

- `RegistrationOrchestrationService` - 注册流程编排
- `UniquenessCheckService` - 唯一性检查

**流程**:

1. 接收注册请求
2. 调用member-service检查唯一性
3. 发送验证码 (存Redis)
4. 验证验证码
5. 调用member-service创建用户
6. 可选: 自动登录返回Token

**数据存储**:

- ✅ Redis: 验证码
- ❌ MySQL: 无

---

### 4.3 权限查询上下文 (Permission Query Context)

**核心职责**: 查询用户权限并缓存

**领域服务**:

- `PermissionQueryService` - 权限查询编排

**流程**:

1. 检查Redis缓存
2. 缓存未命中，调用member-service查询
3. 将结果缓存到Redis (60秒TTL)
4. 返回权限集合

**数据存储**:

- ✅ Redis: 权限缓存
- ❌ MySQL: 无

---

### 4.4 设备查询上下文 (Device Query Context)

**核心职责**: 查询用户设备列表

**领域服务**:

- `DeviceQueryService` - 设备查询编排
- `DeviceKickService` - 设备踢出编排

**流程**:

1. 调用member-service查询设备列表
2. 踢出设备时，撤销该设备的RefreshToken
3. 将Token加入黑名单

**数据存储**:

- ✅ Redis: 在线设备缓存、Token黑名单
- ❌ MySQL: 无

---

## 5. 领域模型设计

### 5.0 Domain Primitive 设计原则 ⭐

#### 5.0.1 什么是Domain Primitive

Domain Primitive（领域原语）是DDD中的重要概念，指将**具有业务含义的基本类型**封装成**不变的值对象**。

**问题**: 当前设计中使用了大量基本类型

```java
public class JwtToken {
    private final String tokenId;        // ❓ String可以是任何值
    private final String userId;         // ❓ String可以是任何值
    private final Long issuedAt;         // ❓ Long可以是任何值
    private final Long expiresAt;        // ❓ Long可以是任何值
}
```

**改进**: 使用Domain Primitive

```java
public class JwtToken {
    private final TokenId tokenId;       // ✅ 有业务含义
    private final UserId userId;         // ✅ 有业务含义
    private final Timestamp issuedAt;    // ✅ 有业务含义
    private final Timestamp expiresAt;   // ✅ 有业务含义
}
```

#### 5.0.2 使用Domain Primitive的判断标准

| 标准       | 说明        | 示例                   |
|----------|-----------|----------------------|
| **业务规则** | 是否有验证规则？  | Email必须符合格式          |
| **业务含义** | 是否有特定含义？  | UserId vs String     |
| **行为**   | 是否有领域行为？  | PhoneNumber.format() |
| **不变性**  | 是否需要保证不变？ | TokenId创建后不变         |
| **复用性**  | 是否多处使用？   | IpAddress在多个上下文      |

#### 5.0.3 Auth-Service中的Domain Primitive分级

##### ⭐ 高价值 - 必须使用Domain Primitive

```java
// 1. Email - 有严格的验证规则
public record Email(String value) {
    public Email {
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidEmailException("邮箱格式不正确: " + value);
        }
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
}

// 2. PhoneNumber - 有国际格式验证
public record PhoneNumber(String value) {
    public PhoneNumber {
        if (!value.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new InvalidPhoneNumberException("手机号格式不正确: " + value);
        }
    }

    public String getCountryCode() {
        // 提取国家代码
    }
}

// 3. IpAddress - 有IPv4/IPv6验证
public record IpAddress(String value) {
    public IpAddress {
        if (!isValidIp(value)) {
            throw new InvalidIpAddressException("IP地址不正确: " + value);
        }
    }

    public boolean isSameRegion(IpAddress other) {
        // 异地登录检测
    }
}

// 4. VerificationCode - 有格式和安全规则
public record VerificationCode(String value) {
    public VerificationCode {
        if (!value.matches("^\\d{6}$")) {
            throw new InvalidCodeException("验证码必须是6位数字");
        }
    }
}

// 5. Password - 有复杂度规则
public record Password(String value) {
    public Password {
        if (value.length() < 8) {
            throw new WeakPasswordException("密码至少8位");
        }
        if (!hasUpperCase(value) || !hasLowerCase(value) || !hasDigit(value)) {
            throw new WeakPasswordException("密码必须包含大小写字母和数字");
        }
    }

    public int calculateStrength() {
        // 计算密码强度
    }
}
```

##### 🟡 中等价值 - 建议使用Domain Primitive

```java
// 1. TokenId - 有业务含义，但验证简单
public record TokenId(String value) {
    public TokenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TokenId不能为空");
        }
    }

    public static TokenId generate() {
        return new TokenId(UUID.randomUUID().toString());
    }
}

// 2. UserId - 有业务含义，便于类型安全
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId不能为空");
        }
    }
}

// 3. DeviceId - 有业务含义
public record DeviceId(String value) {
    public DeviceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeviceId不能为空");
        }
    }
}
```

##### 🟢 低价值 - 可以使用基本类型

```java
// 1. Timestamp - 简单的Long，无复杂验证
private final Long issuedAt;     // ✅ 直接用Long
private final Long expiresAt;    // ✅ 直接用Long

// 如果一定要封装，使用record
public record Timestamp(Long value) {
    public boolean isBefore(Timestamp other) {
        return value < other.value;
    }

    public boolean isAfter(Timestamp other) {
        return value > other.value;
    }
}

// 2. 简单的String标识符（无业务规则）
private final String rawToken;   // ✅ 直接用String

// 3. 计数器
private int verifyAttempts;      // ✅ 直接用int
```

#### 5.0.4 平衡点判断流程

```
┌─────────────────────────────────┐
│ 是否有业务验证规则？              │
│ (Email格式、密码强度等)           │
└────────┬────────────────────────┘
         │
         ├─ YES → ⭐ 必须使用Domain Primitive
         │
         └─ NO → 继续判断
                 │
                 ┌────────────────────────────┐
                 │ 是否有领域行为？            │
                 │ (isSameRegion, getDomain等) │
                 └────────┬───────────────────┘
                          │
                          ├─ YES → 🟡 建议使用Domain Primitive
                          │
                          └─ NO → 继续判断
                                  │
                                  ┌────────────────────────┐
                                  │ 是否在多处使用？        │
                                  │ 是否需要类型安全？      │
                                  └────────┬───────────────┘
                                           │
                                           ├─ YES → 🟡 建议使用Domain Primitive
                                           │
                                           └─ NO → 🟢 使用基本类型
```

#### 5.0.5 推荐的Domain Primitive清单

**Auth-Service应使用的Domain Primitive**:

```java
// ⭐ 高价值 - 必须使用
com.pot.auth.domain.shared.valueobject/
        ├──Email.java              // 邮箱（有验证规则）
├──PhoneNumber.java        // 手机号（有验证规则）
├──Password.java           // 密码（有复杂度规则）
├──IpAddress.java          // IP地址（有格式验证 + 异地检测）
└──VerificationCode.java   // 验证码（有格式验证）

// 🟡 中等价值 - 建议使用
com.pot.auth.domain.shared.valueobject/
        ├──TokenId.java           // Token ID（类型安全）
├──UserId.java            // 用户ID（类型安全 + 业务含义）
├──DeviceId.java          // 设备ID（类型安全）
└──Username.java          // 用户名（有验证规则）

// 🟢 可选 - 基本类型即可
-
Long issuedAt           // 时间戳（简单Long）
-
Long expiresAt          // 时间戳（简单Long）
-
String rawToken         // 原始Token（无业务规则）
-
int verifyAttempts      // 尝试次数（简单计数）
-
boolean verified        // 布尔标志（简单状态）
```

#### 5.0.6 过度使用的反例 ❌

```java
// ❌ 不要为了Domain Primitive而Domain Primitive
public record AttemptCount(int value) {  // 过度封装
    public AttemptCount {
        if (value < 0) throw new IllegalArgumentException();
    }
}

public record IsExpired(boolean value) {  // 过度封装
    // 这只是一个简单的布尔值，没有业务含义
}

public record RawTokenString(String value) {  // 过度封装
    // 这只是一个简单的字符串，没有验证规则
}
```

#### 5.0.7 最佳实践建议

##### ✅ DO - 应该做的

1. **有验证规则的必须封装**

```java
// ✅ Email有格式验证
public record Email(String value) {
    public Email {
        validateFormat(value);
    }
}
```

2. **有领域行为的建议封装**

```java
// ✅ IpAddress有异地检测行为
public record IpAddress(String value) {
    public boolean isSameRegion(IpAddress other) {
        // 业务逻辑
    }
}
```

3. **跨多个上下文使用的建议封装**

```java
// ✅ UserId在多个上下文使用，类型安全
public record UserId(String value) {
}
```

##### ❌ DON'T - 不应该做的

1. **简单计数器不要封装**

```java
// ❌ 过度封装
public record AttemptCount(int value) {
}

// ✅ 直接使用
private int attemptCount;
```

2. **简单布尔标志不要封装**

```java
// ❌ 过度封装
public record IsVerified(boolean value) {
}

// ✅ 直接使用
private boolean isVerified;
```

3. **仅在单个类内部使用的不要封装**

```java
// ❌ 过度封装（只在JwtToken内部使用）
public record RawTokenValue(String value) {
}

// ✅ 直接使用
private String rawToken;
```

#### 5.0.8 重构建议

**当前JwtToken的改进**:

```java
// 改进前
public class JwtToken {
    private final String tokenId;
    private final String userId;
    private final UserDomain userDomain;
    private final TokenType type;
    private final String rawToken;
    private final Set<String> authorities;
    private final LoginContext loginContext;
    private final Long issuedAt;
    private final Long expiresAt;
    private final Long lastRefreshedAt;
}

// 改进后 - 平衡的设计
public class JwtToken {
    private final TokenId tokenId;              // ✅ Domain Primitive（类型安全）
    private final UserId userId;                // ✅ Domain Primitive（类型安全）
    private final UserDomain userDomain;        // ✅ 枚举（已经是Domain Primitive）
    private final TokenType type;               // ✅ 枚举（已经是Domain Primitive）
    private final String rawToken;              // ✅ 基本类型（无业务规则）
    private final Set<String> authorities;      // ✅ 基本类型（简单集合）
    private final LoginContext loginContext;    // ✅ 值对象（已经是复合对象）
    private final Long issuedAt;                // ✅ 基本类型（简单时间戳）
    private final Long expiresAt;               // ✅ 基本类型（简单时间戳）
    private final Long lastRefreshedAt;         // ✅ 基本类型（简单时间戳）

    // 领域行为
    public boolean isExpired(Long currentTimestamp) {
        return currentTimestamp > expiresAt;  // ✅ 简单比较，不需要封装
    }
}
```

**VerificationCode的改进**:

```java
// 改进前
public class VerificationCode {
    private final String codeId;
    private final CodeType codeType;
    private final String recipient;
    private final String code;              // ❓ 应该封装
    private final CodePurpose purpose;
    private final Long createdAt;
    private final Long expiresAt;
    private int verifyAttempts;
    private boolean verified;
}

// 改进后
public class VerificationCode {
    private final CodeId codeId;                    // 🟡 Domain Primitive（可选）
    private final CodeType codeType;                // ✅ 枚举
    private final Email recipient;                  // ⭐ Domain Primitive（有验证）
    private final Code code;                        // ⭐ Domain Primitive（有验证）
    private final CodePurpose purpose;              // ✅ 枚举
    private final Long createdAt;                   // ✅ 基本类型
    private final Long expiresAt;                   // ✅ 基本类型
    private int verifyAttempts;                     // ✅ 基本类型（简单计数）
    private boolean verified;                       // ✅ 基本类型（简单标志）
}

// Code的Domain Primitive
public record Code(String value) {
    public Code {
        if (!value.matches("^\\d{6}$")) {
            throw new InvalidCodeException("验证码必须是6位数字");
        }
    }
}
```

#### 5.0.9 总结：平衡点在哪里？

| 因素       | 使用Domain Primitive | 使用基本类型   |
|----------|--------------------|----------|
| **验证规则** | 有复杂验证              | 无验证或简单验证 |
| **领域行为** | 有领域方法              | 无领域方法    |
| **业务含义** | 有明确业务含义            | 纯技术属性    |
| **复用性**  | 跨多个上下文             | 仅单个类内部   |
| **类型安全** | 需要编译期检查            | 不需要      |
| **代码量**  | 可接受增加              | 追求简洁     |

**推荐原则**:

1. **有验证规则** → 必须使用Domain Primitive ⭐
2. **有领域行为** → 建议使用Domain Primitive 🟡
3. **需要类型安全** → 建议使用Domain Primitive 🟡
4. **简单计数/标志** → 使用基本类型 🟢

**黄金法则**:
> 当你犹豫是否要封装时，问自己：
> 1. 这个字段有业务规则吗？ → 有 → 封装
> 2. 这个字段有领域行为吗？ → 有 → 封装
> 3. 否则 → 不封装

---

### 5.1 认证上下文

#### JwtToken 值对象

```java
// 值对象，不持久化到数据库
public class JwtToken {
    private final TokenId tokenId;              // ✅ Domain Primitive（类型安全）
    private final UserId userId;                // ✅ Domain Primitive（类型安全）
    private final UserDomain userDomain;        // ✅ 枚举（已经是Domain Primitive）
    private final TokenType type;               // ✅ 枚举（已经是Domain Primitive）
    private final String rawToken;              // ✅ 基本类型（无业务规则）
    private final Set<String> authorities;      // ✅ 基本类型（简单集合）
    private final LoginContext loginContext;    // ✅ 值对象（已经是复合对象）
    private final Long issuedAt;                // ✅ 基本类型（简单时间戳）
    private final Long expiresAt;               // ✅ 基本类型（简单时间戳）
    private final Long lastRefreshedAt;         // ✅ 基本类型（简单时间戳）

    // 不变性：值对象创建后不可修改
    // 构造函数私有，通过工厂方法创建

    // 领域行为
    public boolean isExpired(Long currentTimestamp) {
        return currentTimestamp > expiresAt;
    }

    public TokenPair refresh(Long currentTimestamp, JwtTokenService tokenService) {
        if (this.type != TokenType.REFRESH) {
            throw new InvalidTokenOperationException("只能刷新RefreshToken");
        }

        // 生成新的AccessToken
        JwtToken newAccessToken = tokenService.createAccessToken(
                this.userId,
                this.userDomain,
                this.authorities,
                currentTimestamp
        );

        // 滑动窗口续期：7天内刷新，RefreshToken也续期
        if (shouldRenewRefreshToken(currentTimestamp)) {
            JwtToken newRefreshToken = tokenService.createRefreshToken(
                    this.userId,
                    this.userDomain,
                    currentTimestamp
            );
            return new TokenPair(newAccessToken, newRefreshToken, true);
        }

        return new TokenPair(newAccessToken, this, false);
    }

    private boolean shouldRenewRefreshToken(Long currentTimestamp) {
        long timeSinceLastRefresh = currentTimestamp - this.lastRefreshedAt;
        return timeSinceLastRefresh < (7 * 24 * 3600); // 7天
    }

    // 工厂方法
    public static JwtToken create(
            String userId,
            UserDomain userDomain,
            TokenType type,
            Set<String> authorities,
            LoginContext context,
            Long issuedAt,
            Long expiresAt
    ) {
        return new JwtToken(
                UUID.randomUUID().toString(),
                userId,
                userDomain,
                type,
                null, // rawToken由JwtTokenService生成
                authorities,
                context,
                issuedAt,
                expiresAt,
                issuedAt
        );
    }
}

public enum TokenType {
    ACCESS,    // 访问令牌，1小时
    REFRESH    // 刷新令牌，30天
}

public enum UserDomain {
    MEMBER,    // 会员域
    ADMIN      // 后台用户域
}

// 登录上下文值对象
public record LoginContext(
        LoginMethod method,
        String provider,           // oauth2: github/google, wechat: wechat-open
        IpAddress ipAddress,
        String userAgent,
        DeviceInfo deviceInfo,
        Long loginTimestamp
) {
}

public enum LoginMethod {
    PASSWORD,              // 密码登录
    VERIFICATION_CODE,     // 验证码登录
    OAUTH2,                // OAuth2登录
    WECHAT_SCAN            // 微信扫码登录
}

// 设备信息值对象
public record DeviceInfo(
        String deviceId,           // 客户端生成UUID
        DeviceType deviceType,
        String deviceName,
        String osVersion,
        String appVersion
) {
}

public enum DeviceType {
    WEB,
    IOS,
    ANDROID,
    WECHAT_MP,
    WECHAT_H5
}
```

#### TokenBlacklist 值对象

```java
// 值对象，存储在Redis
public class TokenBlacklist {
    private final String tokenJti;
    private final String userId;
    private final UserDomain userDomain;
    private final Long blacklistedAt;
    private final Long expiresAt;              // 原Token过期时间
    private final BlacklistReason reason;

    public boolean isExpired(Long currentTimestamp) {
        return currentTimestamp > expiresAt;
    }

    // 工厂方法
    public static TokenBlacklist fromToken(JwtToken token, BlacklistReason reason) {
        return new TokenBlacklist(
                token.getTokenId(),
                token.getUserId(),
                token.getUserDomain(),
                System.currentTimeMillis() / 1000,
                token.getExpiresAt(),
                reason
        );
    }

    // Redis存储Key
    public String getRedisKey() {
        return "auth:blacklist:" + tokenJti;
    }

    // TTL = 原Token剩余有效期
    public long getTTL(Long currentTimestamp) {
        return expiresAt - currentTimestamp;
    }
}

public enum BlacklistReason {
    LOGOUT,              // 用户主动登出
    DEVICE_KICKED,       // 设备被踢出
    PASSWORD_CHANGED,    // 密码修改
    PERMISSION_CHANGED,  // 权限变更
    SECURITY_CONCERN     // 安全原因
}
```

#### VerificationCode 值对象

```java
// 值对象，存储在Redis
public class VerificationCode {
    private final CodeId codeId;                    // 🟡 Domain Primitive（可选）
    private final CodeType codeType;                // ✅ 枚举
    private final Email recipient;                  // ⭐ Domain Primitive（有验证）
    private final Code code;                        // ⭐ Domain Primitive（有验证）
    private final CodePurpose purpose;              // ✅ 枚举
    private final Long createdAt;                   // ✅ 基本类型
    private final Long expiresAt;                   // ✅ 基本类型
    private int verifyAttempts;                     // ✅ 基本类型（简单计数）
    private boolean verified;                       // ✅ 基本类型（简单标志）
}

// Code的Domain Primitive
public record Code(String value) {
    public Code {
        if (!value.matches("^\\d{6}$")) {
            throw new InvalidCodeException("验证码必须是6位数字");
        }
    }
}
```

### 5.2 领域服务

#### AuthenticationDomainService

```java
public interface AuthenticationDomainService {

    // 1. 密码认证
    AuthenticationResult authenticateWithPassword(
            String identifier,             // 用户名/邮箱/手机号
            String password,
            UserDomain userDomain,
            DeviceInfo deviceInfo,
            IpAddress ipAddress,
            UserModuleAdapter moduleAdapter
    );

    // 2. 验证码认证
    AuthenticationResult authenticateWithVerificationCode(
            String recipient,
            String code,
            UserDomain userDomain,
            DeviceInfo deviceInfo,
            IpAddress ipAddress,
            UserModuleAdapter moduleAdapter
    );

    // 3. OAuth2认证
    AuthenticationResult authenticateWithOAuth2(
            String code,
            OAuth2Provider provider,
            UserDomain userDomain,
            DeviceInfo deviceInfo,
            IpAddress ipAddress,
            UserModuleAdapter moduleAdapter
    );

    // 4. 微信扫码认证
    AuthenticationResult authenticateWithWechatScan(
            String code,
            DeviceInfo deviceInfo,
            IpAddress ipAddress,
            UserModuleAdapter moduleAdapter
    );
}

// 认证结果值对象
public record AuthenticationResult(
        String userId,
        UserDomain userDomain,
        String username,
        Set<String> authorities,
        TokenPair tokenPair,
        boolean isSuspiciousLogin,
        Long authenticatedAt
) {
}

public record TokenPair(
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt,
        boolean refreshTokenRenewed
) {
}
```

#### JwtTokenService

```java
public interface JwtTokenService {

    // 生成AccessToken
    JwtToken createAccessToken(
            String userId,
            UserDomain userDomain,
            Set<String> authorities,
            Long issuedAt
    );

    // 生成RefreshToken
    JwtToken createRefreshToken(
            String userId,
            UserDomain userDomain,
            Long issuedAt
    );

    // 验证Token
    Optional<JwtToken> validateToken(String rawToken);

    // 将Token加入黑名单
    void revokeToken(JwtToken token, BlacklistReason reason);

    // 检查Token是否在黑名单
    boolean isBlacklisted(String jti);
}
```

#### RegistrationOrchestrationService

```java
public interface RegistrationOrchestrationService {

    // 密码注册
    RegistrationResult registerWithPassword(
            String username,
            String email,
            String phone,
            String password,
            UserDomain userDomain,
            UserModuleAdapter moduleAdapter
    );

    // 验证码注册 (自动登录)
    AuthenticationResult registerWithVerificationCode(
            String recipient,
            String code,
            UserDomain userDomain,
            DeviceInfo deviceInfo,
            UserModuleAdapter moduleAdapter
    );

    // OAuth2注册 (自动登录)
    AuthenticationResult registerWithOAuth2(
            OAuth2UserInfo userInfo,
            UserDomain userDomain,
            DeviceInfo deviceInfo,
            UserModuleAdapter moduleAdapter
    );

    // 发送验证邮件
    void sendVerificationEmail(String email, String verificationToken);

    // 发送验证短信
    void sendVerificationSms(String phone, String verificationCode);

    // 验证邮箱/手机号
    void verifyContact(String token, ContactType type, UserModuleAdapter moduleAdapter);
}

public record RegistrationResult(
        String userId,
        String username,
        RegistrationStatus status,
        String message
) {
}

public enum RegistrationStatus {
    PENDING_VERIFICATION,  // 待验证
    COMPLETED              // 已完成
}
```

#### PermissionQueryService

```java
public interface PermissionQueryService {

    // 查询用户权限 (带缓存)
    Set<String> getPermissions(
            String userId,
            UserDomain userDomain,
            UserModuleAdapter moduleAdapter
    );

    // 批量查询用户权限
    Map<String, Set<String>> getPermissionsBatch(
            List<String> userIds,
            UserDomain userDomain,
            UserModuleAdapter moduleAdapter
    );

    // 检查用户是否有指定权限
    boolean hasPermission(
            String userId,
            UserDomain userDomain,
            String permissionCode,
            UserModuleAdapter moduleAdapter
    );

    // 清除权限缓存
    void evictPermissionCache(String userId, UserDomain userDomain);
}
```

---

## 6. 防腐层设计 ⭐⭐⭐

> **设计原则**: 领域层不应该直接依赖任何外部框架或服务的具体实现  
> **核心思想**: 通过端口-适配器模式（六边形架构），为所有外部依赖建立防腐层  
> **扩展性目标**: 可以无缝切换底层技术实现，而不影响领域层代码

### 6.0 防腐层架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                     领域层 (Domain Layer)                        │
│                   纯业务逻辑，不依赖具体技术                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 领域服务只依赖端口接口 (Port Interface)                   │  │
│  │ - AuthenticationDomainService                              │  │
│  │ - RegistrationOrchestrationService                         │  │
│  │ - PermissionQueryService                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ▼ 依赖                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 端口接口层 (Port Interfaces - 领域层定义)                 │  │
│  │ ✅ CachePort                  - 缓存端口                   │  │
│  │ ✅ UserModulePort             - 用户模块端口               │  │
│  │ ✅ TokenManagementPort        - Token管理端口              │  │
│  │ ✅ NotificationPort           - 通知端口                   │  │
│  │ ✅ DistributedLockPort        - 分布式锁端口               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               ▲ 实现
┌─────────────────────────────────────────────────────────────────┐
│                  基础设施层 (Infrastructure Layer)               │
│                   适配器实现，依赖具体技术框架                    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 适配器实现层 (Adapter Implementations)                     │  │
│  │                                                            │  │
│  │ 缓存适配器:                                                │  │
│  │ ├─ RedisCacheAdapter          → Spring Data Redis        │  │
│  │ ├─ LocalCacheAdapter           → Caffeine/Guava          │  │
│  │ └─ CompositeCacheAdapter       → L1 + L2 组合缓存        │  │
│  │                                                            │  │
│  │ 用户模块适配器: ⭐⭐⭐                                      │  │
│  │ ├─ MemberModuleAdapter         → member-facade (jar依赖) │  │
│  │ │  └─ 转换facade DTO → 领域层UserDTO (防腐)              │  │
│  │ └─ AdminModuleAdapter          → admin-facade (jar依赖)  │  │
│  │    └─ 转换facade DTO → 领域层UserDTO (防腐)              │  │
│  │                                                            │  │
│  │ Token管理适配器:                                           │  │
│  │ ├─ SpringSecurityJwtAdapter    → Spring Security 6.x     │  │
│  │ └─ ShiroJwtAdapter             → Apache Shiro (预留)     │  │
│  │                                                            │  │
│  │ 通知适配器:                                                │  │
│  │ ├─ SmsNotificationAdapter      → 阿里云短信              │  │
│  │ ├─ EmailNotificationAdapter    → SMTP / 腾讯邮件         │  │
│  │ └─ PushNotificationAdapter     → framework-starter-touch │  │
│  │                                                            │  │
│  │ 分布式锁适配器:                                            │  │
│  │ ├─ RedisDistributedLockAdapter → Redisson                │  │
│  │ └─ LocalLockAdapter            → ReentrantLock (测试用)  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Feign Client定义 (调用外部服务)                           │  │
│  │ ├─ MemberServiceClient         → member-facade           │  │
│  │ │  使用member-facade的接口和DTO                          │  │
│  │ └─ AdminServiceClient          → admin-facade            │  │
│  │    使用admin-facade的接口和DTO                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            │ HTTP调用                            │
└────────────────────────────┼───────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   外部服务 (External Services)                   │
│                                                                  │
│  ┌────────────────────┐         ┌────────────────────┐         │
│  │  member-service    │         │  admin-service     │         │
│  │  实现member-facade │         │  实现admin-facade  │         │
│  └────────────────────┘         └────────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

**关键设计原则**：

1. ✅ auth-service依赖member-facade.jar（共享API契约）
2. ✅ Feign Client使用facade的接口和DTO
3. ✅ Adapter仍然做DTO转换（facade DTO → 领域层DTO）
4. ✅ 领域层完全不知道facade的存在

### 6.0.1 为什么需要防腐层？

#### ❌ 反例：领域层直接依赖具体实现

```java
// ❌ 错误示例：领域服务直接依赖Redis
@Service
public class AuthenticationDomainService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;  // ❌ 强依赖Redis

    @Autowired
    private JwtEncoder jwtEncoder;  // ❌ 强依赖Spring Security

    @Autowired
    private MemberServiceClient memberClient;  // ❌ 强依赖Feign Client

    public AuthenticationResult login(String username, String password) {
        // 1. 从Redis获取验证码
        String code = redisTemplate.opsForValue().get("code:" + username);  // ❌ 直接使用Redis API

        // 2. 调用member-service验证
        MemberDTO member = memberClient.authenticate(username, password);  // ❌ 直接使用Feign

        // 3. 生成JWT Token
        String token = jwtEncoder.encode(...);  // ❌ 直接使用Spring Security

        // 4. 缓存权限到Redis
        redisTemplate.opsForValue().set("perm:" + member.getId(), permissions, 60, TimeUnit.SECONDS);  // ❌

        return new AuthenticationResult(token);
    }
}

// 问题：
// 1. 如果要从Redis切换到Caffeine，需要修改领域服务代码
// 2. 如果要从Spring Security切换到Shiro，需要修改领域服务代码
// 3. 领域逻辑与技术实现强耦合，难以测试
// 4. 违反依赖倒置原则 (DIP)
```

#### ✅ 正例：领域层依赖抽象接口

```java
// ✅ 正确示例：领域服务依赖端口接口
@Service
public class AuthenticationDomainService {

    private final CachePort cachePort;                    // ✅ 依赖抽象
    private final TokenManagementPort tokenPort;          // ✅ 依赖抽象
    private final UserModulePort userModulePort;          // ✅ 依赖抽象

    public AuthenticationResult login(String username, String password) {
        // 1. 从缓存获取验证码
        Optional<String> code = cachePort.get("code:" + username, String.class);  // ✅ 抽象接口

        // 2. 调用用户模块验证
        UserDTO user = userModulePort.authenticateWithPassword(username, password);  // ✅ 抽象接口

        // 3. 生成Token
        JwtToken token = tokenPort.generateAccessToken(user);  // ✅ 抽象接口

        // 4. 缓存权限
        cachePort.set("perm:" + user.getId(), permissions, Duration.ofSeconds(60));  // ✅ 抽象接口

        return new AuthenticationResult(token);
    }
}

// 优势：
// 1. ✅ 可以通过配置切换缓存实现（Redis → Caffeine → Composite）
// 2. ✅ 可以通过配置切换安全框架（Spring Security → Shiro）
// 3. ✅ 领域逻辑与技术实现解耦，易于单元测试（使用Mock）
// 4. ✅ 符合依赖倒置原则 (DIP)
// 5. ✅ 符合开闭原则 (OCP)
```

---

### 6.1 缓存防腐层 (Cache Anti-Corruption Layer)

#### 6.1.1 CachePort 接口定义

```java
// 端口接口 - 放在 domain/port 包
package com.pot.auth.domain.port;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 缓存端口接口
 * 领域层通过此接口访问缓存，不依赖具体实现
 */
public interface CachePort {

    // ========== 基本操作 ==========

    /**
     * 设置缓存
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 获取缓存
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 批量删除
     */
    void deleteBatch(Set<String> keys);

    /**
     * 检查是否存在
     */
    boolean exists(String key);

    // ========== 集合操作 ==========

    /**
     * 添加到集合
     */
    <T> void addToSet(String key, T value, Duration ttl);

    /**
     * 从集合移除
     */
    <T> void removeFromSet(String key, T value);

    /**
     * 获取集合所有成员
     */
    <T> Set<T> getSet(String key, Class<T> type);

    /**
     * 检查集合成员
     */
    <T> boolean isMemberOfSet(String key, T value);

    // ========== Hash操作 ==========

    /**
     * 设置Hash字段
     */
    <T> void setHash(String key, String field, T value, Duration ttl);

    /**
     * 获取Hash字段
     */
    <T> Optional<T> getHash(String key, String field, Class<T> type);

    /**
     * 获取整个Hash
     */
    <T> Map<String, T> getAllHash(String key, Class<T> type);

    /**
     * 删除Hash字段
     */
    void deleteHash(String key, String field);

    // ========== 原子操作 ==========

    /**
     * 原子递增
     */
    long increment(String key, long delta, Duration ttl);

    /**
     * 原子递减
     */
    long decrement(String key, long delta);

    /**
     * 设置NX (不存在时设置)
     */
    <T> boolean setIfAbsent(String key, T value, Duration ttl);

    // ========== TTL管理 ==========

    /**
     * 设置过期时间
     */
    void expire(String key, Duration ttl);

    /**
     * 获取剩余TTL
     */
    Optional<Duration> getTtl(String key);

    /**
     * 移除过期时间
     */
    void persist(String key);
}
```

#### 6.1.2 Redis适配器实现

```java
// Redis实现 - 放在 infrastructure/adapter/cache 包
package com.pot.auth.infrastructure.adapter.cache;

import com.pot.auth.domain.port.CachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存适配器
 * 将CachePort接口适配到Spring Data Redis
 */
@Component
@ConditionalOnProperty(name = "auth.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheAdapter implements CachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable((T) value);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void deleteBatch(Set<String> keys) {
        redisTemplate.delete(keys);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public <T> void addToSet(String key, T value, Duration ttl) {
        redisTemplate.opsForSet().add(key, value);
        redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSet(String key, Class<T> type) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return (Set<T>) members;
    }

    @Override
    public <T> boolean isMemberOfSet(String key, T value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public <T> void setHash(String key, String field, T value, Duration ttl) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getHash(String key, String field, Class<T> type) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return Optional.ofNullable((T) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAllHash(String key, Class<T> type) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, T> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), (T) v));
        return result;
    }

    @Override
    public void deleteHash(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    @Override
    public long increment(String key, long delta, Duration ttl) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
        return result != null ? result : 0;
    }

    @Override
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().decrement(key, delta);
        return result != null ? result : 0;
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<Duration> getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttl == null || ttl < 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(ttl));
    }

    @Override
    public void persist(String key) {
        redisTemplate.persist(key);
    }
}
```

#### 6.1.3 本地缓存适配器实现 (Caffeine)

```java
package com.pot.auth.infrastructure.adapter.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.pot.auth.domain.port.CachePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地缓存适配器 (使用Caffeine)
 * 适用于单机部署或测试环境
 */
@Component
@ConditionalOnProperty(name = "auth.cache.type", havingValue = "local")
public class LocalCacheAdapter implements CachePort {

    private final Cache<String, CacheEntry> cache;
    private final Map<String, Set<Object>> sets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> hashes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public LocalCacheAdapter() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new Expiry<String, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(String key, CacheEntry value, long currentTime) {
                        return value.getTtl().toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, CacheEntry value, long currentTime, long currentDuration) {
                        return value.getTtl().toNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, CacheEntry value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        cache.put(key, new CacheEntry(value, ttl));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry entry = cache.getIfPresent(key);
        return Optional.ofNullable(entry).map(e -> (T) e.getValue());
    }

    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    @Override
    public void deleteBatch(Set<String> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public boolean exists(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public <T> void addToSet(String key, T value, Duration ttl) {
        sets.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(value);
        cache.put(key + ":set", new CacheEntry(true, ttl));
    }

    @Override
    public <T> void removeFromSet(String key, T value) {
        Set<Object> set = sets.get(key);
        if (set != null) {
            set.remove(value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSet(String key, Class<T> type) {
        Set<Object> set = sets.get(key);
        return set != null ? (Set<T>) set : Collections.emptySet();
    }

    @Override
    public <T> boolean isMemberOfSet(String key, T value) {
        Set<Object> set = sets.get(key);
        return set != null && set.contains(value);
    }

    @Override
    public <T> void setHash(String key, String field, T value, Duration ttl) {
        hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
        cache.put(key + ":hash", new CacheEntry(true, ttl));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getHash(String key, String field, Class<T> type) {
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) hash.get(field));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAllHash(String key, Class<T> type) {
        Map<String, Object> hash = hashes.get(key);
        return hash != null ? (Map<String, T>) hash : Collections.emptyMap();
    }

    @Override
    public void deleteHash(String key, String field) {
        Map<String, Object> hash = hashes.get(key);
        if (hash != null) {
            hash.remove(field);
        }
    }

    @Override
    public long increment(String key, long delta, Duration ttl) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        long result = counter.addAndGet(delta);
        cache.put(key + ":counter", new CacheEntry(true, ttl));
        return result;
    }

    @Override
    public long decrement(String key, long delta) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        return counter.addAndGet(-delta);
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        CacheEntry existing = cache.getIfPresent(key);
        if (existing == null) {
            cache.put(key, new CacheEntry(value, ttl));
            return true;
        }
        return false;
    }

    @Override
    public void expire(String key, Duration ttl) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null) {
            cache.put(key, new CacheEntry(entry.getValue(), ttl));
        }
    }

    @Override
    public Optional<Duration> getTtl(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        return Optional.ofNullable(entry).map(CacheEntry::getTtl);
    }

    @Override
    public void persist(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null) {
            cache.put(key, new CacheEntry(entry.getValue(), Duration.ofDays(365)));
        }
    }

    // 内部类：缓存条目
    private static class CacheEntry {
        private final Object value;
        private final Duration ttl;

        public CacheEntry(Object value, Duration ttl) {
            this.value = value;
            this.ttl = ttl;
        }

        public Object getValue() {
            return value;
        }

        public Duration getTtl() {
            return ttl;
        }
    }
}
```

#### 6.1.4 组合缓存适配器 (L1 + L2)

```java
package com.pot.auth.infrastructure.adapter.cache;

import com.pot.auth.domain.port.CachePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 组合缓存适配器 (L1本地 + L2 Redis)
 * L1: Caffeine本地缓存 (快速访问，但不共享)
 * L2: Redis分布式缓存 (共享，但网络开销)
 */
@Component
@ConditionalOnProperty(name = "auth.cache.type", havingValue = "composite")
public class CompositeCacheAdapter implements CachePort {

    private final LocalCacheAdapter l1Cache;  // 本地缓存
    private final RedisCacheAdapter l2Cache;  // 分布式缓存

    public CompositeCacheAdapter(LocalCacheAdapter l1Cache, RedisCacheAdapter l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        l1Cache.set(key, value, ttl);  // 写入L1
        l2Cache.set(key, value, ttl);  // 写入L2
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        // 先查L1
        Optional<T> l1Result = l1Cache.get(key, type);
        if (l1Result.isPresent()) {
            return l1Result;
        }

        // L1未命中，查L2
        Optional<T> l2Result = l2Cache.get(key, type);
        if (l2Result.isPresent()) {
            // 回写L1
            Duration ttl = l2Cache.getTtl(key).orElse(Duration.ofMinutes(10));
            l1Cache.set(key, l2Result.get(), ttl);
        }

        return l2Result;
    }

    @Override
    public void delete(String key) {
        l1Cache.delete(key);
        l2Cache.delete(key);
    }

    @Override
    public void deleteBatch(Set<String> keys) {
        l1Cache.deleteBatch(keys);
        l2Cache.deleteBatch(keys);
    }

    @Override
    public boolean exists(String key) {
        return l1Cache.exists(key) || l2Cache.exists(key);
    }

    // ... 其他方法类似实现
}
```

#### 6.1.5 配置示例

```yaml
# application.yml

# 使用Redis缓存
auth:
  cache:
    type: redis  # 默认值

---
# 使用本地缓存 (适合单机或测试)
auth:
  cache:
    type: local

---
# 使用组合缓存 (L1本地 + L2 Redis)
auth:
  cache:
    type: composite
```

#### 6.1.6 领域服务使用示例

```java

@Service
public class AuthenticationDomainService {

    private final CachePort cachePort;  // ✅ 只依赖抽象接口

    public void cacheVerificationCode(String recipient, String code) {
        // ✅ 不关心底层是Redis还是Caffeine
        cachePort.set("auth:code:" + recipient, code, Duration.ofMinutes(5));
    }

    public Optional<String> getVerificationCode(String recipient) {
        // ✅ 不关心底层是Redis还是Caffeine
        return cachePort.get("auth:code:" + recipient, String.class);
    }

    public void addToBlacklist(String tokenId, Duration remainingTtl) {
        // ✅ 不关心底层是Redis还是Caffeine
        cachePort.set("auth:blacklist:" + tokenId, true, remainingTtl);
    }
}
```

---

### 6.2 用户模块防腐层 (User Module Anti-Corruption Layer)

#### 6.2.1 UserModulePort 接口定义

```java
// 防腐层接口，隔离auth-service与member/admin-service
public interface UserModuleAdapter {

    // ========== 用户认证 ==========

    // 密码认证
    Optional<UserDTO> authenticateWithPassword(String identifier, String password);

    // 获取用户信息
    Optional<UserDTO> findById(String userId);

    Optional<UserDTO> findByIdentifier(String identifier);

    // ========== 用户创建 ==========

    // 创建用户
    String createUser(CreateUserRequest request);

    // 唯一性检查
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ========== 密码管理 ==========

    // 更新密码
    void updatePassword(String userId, String newPassword);

    // ========== 账户管理 ==========

    // 锁定/解锁账户
    void lockAccount(String userId);

    void unlockAccount(String userId);

    // 记录登录尝试
    void recordLoginAttempt(String userId, boolean success, IpAddress ip, Long timestamp);

    // ========== 权限查询 ==========

    // 查询用户权限
    Set<String> getPermissions(String userId);

    // 查询用户角色
    Set<RoleDTO> getRoles(String userId);

    // 批量查询权限
    Map<String, Set<String>> getPermissionsBatch(List<String> userIds);

    // ========== 设备管理 ==========

    // 查询用户设备列表
    List<DeviceDTO> getDevices(String userId);

    // 记录设备登录
    void recordDeviceLogin(String userId, DeviceInfo deviceInfo, IpAddress ip, String refreshToken);

    // 踢出设备
    void kickDevice(String userId, String deviceId);

    // ========== OAuth2绑定 ==========

    // 查询OAuth2绑定
    Optional<String> findUserIdByOAuth2(String provider, String providerId);

    // 绑定OAuth2账号
    void bindOAuth2(String userId, String provider, String providerId, OAuth2UserInfo userInfo);
}
```

### 6.2 MemberServiceClient (Feign)

#### 6.2.1 UserModulePort 接口定义

```java
// 端口接口 - 放在 domain/port 包
package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.*;

import java.util.*;

/**
 * 用户模块端口接口
 * 领域层通过此接口访问用户模块(member/admin-service)，不依赖Feign
 */
public interface UserModulePort {

    /**
     * 标识当前适配器支持的用户域
     */
    UserDomain supportedDomain();

    // ========== 用户认证 ==========

    /**
     * 密码认证
     */
    Optional<UserDTO> authenticateWithPassword(String identifier, String password);

    /**
     * 获取用户信息
     */
    Optional<UserDTO> findById(UserId userId);

    Optional<UserDTO> findByIdentifier(String identifier);

    // ========== 用户创建 ==========

    /**
     * 创建用户
     */
    UserId createUser(CreateUserRequest request);

    /**
     * 唯一性检查
     */
    boolean existsByUsername(String username);

    boolean existsByEmail(Email email);

    boolean existsByPhone(PhoneNumber phone);

    // ========== 密码管理 ==========

    /**
     * 更新密码
     */
    void updatePassword(UserId userId, Password newPassword);

    // ========== 账户管理 ==========

    /**
     * 锁定/解锁账户
     */
    void lockAccount(UserId userId);

    void unlockAccount(UserId userId);

    /**
     * 记录登录尝试
     */
    void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp);

    // ========== 权限查询 ==========

    /**
     * 查询用户权限
     */
    Set<String> getPermissions(UserId userId);

    /**
     * 查询用户角色
     */
    Set<RoleDTO> getRoles(UserId userId);

    /**
     * 批量查询权限
     */
    Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds);

    // ========== 设备管理 ==========

    /**
     * 查询用户设备列表
     */
    List<DeviceDTO> getDevices(UserId userId);

    /**
     * 记录设备登录
     */
    void recordDeviceLogin(UserId userId, DeviceInfo deviceInfo, IpAddress ip, String refreshToken);

    /**
     * 踢出设备
     */
    void kickDevice(UserId userId, DeviceId deviceId);

    // ========== OAuth2绑定 ==========

    /**
     * 查询OAuth2绑定
     */
    Optional<UserId> findUserIdByOAuth2(String provider, String providerId);

    /**
     * 绑定OAuth2账号
     */
    void bindOAuth2(UserId userId, String provider, String providerId, OAuth2UserInfo userInfo);
}
```

#### 6.2.2 UserModulePortFactory 适配器工厂

```java
package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户模块端口工厂
 * 根据UserDomain动态获取对应的适配器
 */
@Component
public class UserModulePortFactory {

    private final Map<UserDomain, UserModulePort> adapters;

    public UserModulePortFactory(List<UserModulePort> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        UserModulePort::supportedDomain,
                        adapter -> adapter
                ));
    }

    /**
     * 获取指定域的适配器
     */
    public UserModulePort getPort(UserDomain domain) {
        UserModulePort adapter = adapters.get(domain);
        if (adapter == null) {
            throw new UnsupportedUserDomainException("不支持的用户域: " + domain);
        }
        return adapter;
    }

    /**
     * 检查是否支持指定域
     */
    public boolean supports(UserDomain domain) {
        return adapters.containsKey(domain);
    }

    /**
     * 获取所有支持的域
     */
    public Set<UserDomain> getSupportedDomains() {
        return adapters.keySet();
    }
}
```

#### 6.2.3 MemberModuleAdapter 实现

```java
// 适配器实现 - 放在 infrastructure/adapter/usermodule 包
package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.shared.valueobject.*;
import com.pot.auth.infrastructure.client.MemberServiceClient;
// ✅ 使用member-facade的DTO
import com.pot.member.facade.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Member域适配器
 *
 * 职责：
 * 1. 使用member-facade的Feign Client调用member-service
 * 2. 将member-facade的DTO转换成auth领域层的DTO (防腐层)
 * 3. 实现UserModulePort接口
 */
@Component
public class MemberModuleAdapter implements UserModulePort {

    private final MemberServiceClient memberClient;

    public MemberModuleAdapter(MemberServiceClient memberClient) {
        this.memberClient = memberClient;
    }

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.MEMBER;
    }

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        try {
            // 1. 构造member-facade的请求DTO
            AuthenticateRequest facadeRequest = new AuthenticateRequest();
            facadeRequest.setIdentifier(identifier);
            facadeRequest.setPassword(password);

            // 2. 调用Feign Client (使用member-facade的接口)
            AuthenticateResponse facadeResponse = memberClient.authenticate(facadeRequest);

            // 3. 防腐层转换：facade DTO → 领域层DTO
            UserDTO domainUserDTO = convertToUserDTO(facadeResponse.getMember());

            return Optional.of(domainUserDTO);

        } catch (FeignException.NotFound | FeignException.Unauthorized e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findById(UserId userId) {
        try {
            // 使用member-facade的DTO
            MemberDTO facadeMemberDTO = memberClient.getById(userId.value());

            // 防腐层转换
            UserDTO domainUserDTO = convertToUserDTO(facadeMemberDTO);

            return Optional.of(domainUserDTO);
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findByIdentifier(String identifier) {
        try {
            MemberDTO facadeMemberDTO = memberClient.getByIdentifier(identifier);
            UserDTO domainUserDTO = convertToUserDTO(facadeMemberDTO);
            return Optional.of(domainUserDTO);
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public UserId createUser(CreateUserRequest request) {
        // 1. 构造member-facade的请求DTO
        CreateMemberRequest facadeRequest = new CreateMemberRequest();
        facadeRequest.setUsername(request.username());
        facadeRequest.setEmail(request.email().value());
        facadeRequest.setPhone(request.phone().value());
        facadeRequest.setPassword(request.password().value());

        // 2. 调用member-service
        CreateMemberResponse facadeResponse = memberClient.create(facadeRequest);

        // 3. 返回领域层的UserId
        return new UserId(facadeResponse.getUserId());
    }

    @Override
    public boolean existsByUsername(String username) {
        return memberClient.exists("username", username);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return memberClient.exists("email", email.value());
    }

    @Override
    public boolean existsByPhone(PhoneNumber phone) {
        return memberClient.exists("phone", phone.value());
    }

    @Override
    public void updatePassword(UserId userId, Password newPassword) {
        UpdatePasswordRequest facadeRequest = new UpdatePasswordRequest();
        facadeRequest.setNewPassword(newPassword.value());

        memberClient.updatePassword(userId.value(), facadeRequest);
    }

    @Override
    public void lockAccount(UserId userId) {
        memberClient.lockAccount(userId.value());
    }

    @Override
    public void unlockAccount(UserId userId) {
        memberClient.unlockAccount(userId.value());
    }

    @Override
    public void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp) {
        LoginAttemptRequest facadeRequest = new LoginAttemptRequest();
        facadeRequest.setSuccess(success);
        facadeRequest.setIpAddress(ip.value());
        facadeRequest.setTimestamp(timestamp);

        memberClient.recordLoginAttempt(userId.value(), facadeRequest);
    }

    @Override
    public Set<String> getPermissions(UserId userId) {
        return memberClient.getPermissions(userId.value());
    }

    @Override
    public Set<RoleDTO> getRoles(UserId userId) {
        // 使用member-facade的RoleDTO
        Set<com.pot.member.facade.dto.RoleDTO> facadeRoles = memberClient.getRoles(userId.value());

        // 转换成auth领域层的RoleDTO
        return facadeRoles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds) {
        List<String> ids = userIds.stream().map(UserId::value).toList();
        Map<String, Set<String>> facadeResult = memberClient.getPermissionsBatch(ids);

        return facadeResult.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> new UserId(e.getKey()),
                        Map.Entry::getValue
                ));
    }

    @Override
    public List<DeviceDTO> getDevices(UserId userId) {
        // 使用member-facade的DeviceDTO
        List<com.pot.member.facade.dto.DeviceDTO> facadeDevices = memberClient.getDevices(userId.value());

        // 转换成auth领域层的DeviceDTO
        return facadeDevices.stream()
                .map(this::convertToDeviceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void recordDeviceLogin(UserId userId, DeviceInfo deviceInfo, IpAddress ip, String refreshToken) {
        DeviceLoginRequest facadeRequest = new DeviceLoginRequest();
        facadeRequest.setDeviceType(deviceInfo.deviceType());
        facadeRequest.setOs(deviceInfo.os());
        facadeRequest.setBrowser(deviceInfo.browser());
        facadeRequest.setIpAddress(ip.value());
        facadeRequest.setRefreshToken(refreshToken);

        memberClient.recordDeviceLogin(userId.value(), facadeRequest);
    }

    @Override
    public void kickDevice(UserId userId, DeviceId deviceId) {
        memberClient.kickDevice(userId.value(), deviceId.value());
    }

    @Override
    public Optional<UserId> findUserIdByOAuth2(String provider, String providerId) {
        try {
            String userId = memberClient.findUserIdByOAuth2(provider, providerId);
            return Optional.of(new UserId(userId));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public void bindOAuth2(UserId userId, String provider, String providerId, OAuth2UserInfo userInfo) {
        BindOAuth2Request facadeRequest = new BindOAuth2Request();
        facadeRequest.setProvider(provider);
        facadeRequest.setProviderId(providerId);
        facadeRequest.setNickname(userInfo.nickname());
        facadeRequest.setAvatar(userInfo.avatar());

        memberClient.bindOAuth2(userId.value(), facadeRequest);
    }

    // ========== 防腐层：DTO转换 ⭐⭐⭐ ==========

    /**
     * 防腐层：将member-facade的MemberDTO转换为auth领域层的UserDTO
     *
     * 为什么需要转换？
     * 1. 领域纯粹：领域层不应该依赖member-facade的DTO
     * 2. 独立演进：member-facade的DTO改变时，只需修改此处
     * 3. 语义差异：member的"Member"和auth的"User"可能语义不同
     * 4. 字段差异：领域层可能只需要部分字段
     */
    private UserDTO convertToUserDTO(com.pot.member.facade.dto.MemberDTO facadeMemberDTO) {
        return new UserDTO(
                new UserId(facadeMemberDTO.getUserId()),
                facadeMemberDTO.getUsername(),
                new Email(facadeMemberDTO.getEmail()),
                new PhoneNumber(facadeMemberDTO.getPhone()),
                facadeMemberDTO.getStatus(),
                facadeMemberDTO.getCreatedAt(),
                facadeMemberDTO.getUpdatedAt()
        );
    }

    private RoleDTO convertToRoleDTO(com.pot.member.facade.dto.RoleDTO facadeRoleDTO) {
        return new RoleDTO(
                facadeRoleDTO.getRoleId(),
                facadeRoleDTO.getRoleName(),
                facadeRoleDTO.getRoleCode()
        );
    }

    private DeviceDTO convertToDeviceDTO(com.pot.member.facade.dto.DeviceDTO facadeDeviceDTO) {
        return new DeviceDTO(
                new DeviceId(facadeDeviceDTO.getDeviceId()),
                facadeDeviceDTO.getDeviceType(),
                facadeDeviceDTO.getOs(),
                facadeDeviceDTO.getBrowser(),
                new IpAddress(facadeDeviceDTO.getLastIp()),
                facadeDeviceDTO.getLastLoginAt()
        );
    }
}
```

#### 6.2.4 AdminModuleAdapter 实现 (预留)

```java
package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.infrastructure.client.AdminServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Admin域适配器 (预留)
 * 将UserModulePort接口适配到admin-service的Feign Client
 */
@Component
@ConditionalOnProperty(name = "auth.user-domain.admin.enabled", havingValue = "true")
public class AdminModuleAdapter implements UserModulePort {

    private final AdminServiceClient adminClient;

    public AdminModuleAdapter(AdminServiceClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.ADMIN;
    }

    // ... 类似MemberModuleAdapter的实现
}
```

---

### 6.3 Token管理防腐层 (Token Management Anti-Corruption Layer)

#### 6.3.1 TokenManagementPort 接口定义

```java
// 端口接口 - 放在 domain/port 包
package com.pot.auth.domain.port;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.shared.valueobject.*;

import java.util.Optional;
import java.util.Set;

/**
 * Token管理端口接口
 * 领域层通过此接口进行Token管理，不依赖具体的安全框架(Spring Security/Shiro)
 */
public interface TokenManagementPort {

    // ========== Token生成 ==========

    /**
     * 生成访问令牌
     */
    JwtToken generateAccessToken(
            UserId userId,
            UserDomain userDomain,
            String username,
            Set<String> authorities,
            LoginContext loginContext
    );

    /**
     * 生成刷新令牌
     */
    JwtToken generateRefreshToken(
            UserId userId,
            UserDomain userDomain,
            String username,
            LoginContext loginContext
    );

    // ========== Token验证 ==========

    /**
     * 验证Token签名和有效期
     */
    Optional<JwtToken> verifyToken(String rawToken);

    /**
     * 解析Token (不验证签名，用于已过期Token的解析)
     */
    Optional<JwtToken> parseToken(String rawToken);

    // ========== Token刷新 ==========

    /**
     * 刷新访问令牌
     * 使用刷新令牌生成新的访问令牌
     */
    JwtToken refreshAccessToken(JwtToken refreshToken, Set<String> authorities);

    // ========== 密钥管理 ==========

    /**
     * 获取公钥 (用于Gateway本地验证)
     */
    String getPublicKey();

    /**
     * 轮换密钥 (定期更新RSA密钥对)
     */
    void rotateKeys();
}
```

#### 6.3.2 SpringSecurityJwtAdapter 实现

```java
// 适配器实现 - 放在 infrastructure/adapter/token 包
package com.pot.auth.infrastructure.adapter.token;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.authentication.valueobject.*;
import com.pot.auth.domain.shared.valueobject.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;

/**
 * Spring Security JWT适配器
 * 将TokenManagementPort接口适配到Spring Security OAuth2的JWT实现
 */
@Component
@ConditionalOnProperty(name = "auth.token.provider", havingValue = "spring-security", matchIfMissing = true)
public class SpringSecurityJwtAdapter implements TokenManagementPort {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public SpringSecurityJwtAdapter(
            JwtProperties jwtProperties
    ) throws NoSuchAlgorithmException {
        // 初始化RSA密钥对
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        this.accessTokenTtlSeconds = jwtProperties.getAccessTokenTtl().toSeconds();
        this.refreshTokenTtlSeconds = jwtProperties.getRefreshTokenTtl().toSeconds();
    }

    @Override
    public JwtToken generateAccessToken(
            UserId userId,
            UserDomain userDomain,
            String username,
            Set<String> authorities,
            LoginContext loginContext
    ) {
        TokenId tokenId = TokenId.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);

        try {
            // 构建JWT Claims
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .jwtID(tokenId.value())
                    .subject(userId.value())
                    .claim("userDomain", userDomain.name())
                    .claim("username", username)
                    .claim("authorities", new ArrayList<>(authorities))
                    .claim("deviceId", loginContext.deviceId().value())
                    .claim("ip", loginContext.ip().value())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .build();

            // 签名
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claims
            );
            signedJWT.sign(new RSASSASigner(privateKey));

            String rawToken = signedJWT.serialize();

            return new JwtToken(
                    tokenId,
                    userId,
                    userDomain,
                    TokenType.ACCESS_TOKEN,
                    rawToken,
                    authorities,
                    loginContext,
                    now.getEpochSecond(),
                    expiresAt.getEpochSecond(),
                    now.getEpochSecond()
            );

        } catch (JOSEException e) {
            throw new TokenGenerationException("生成Access Token失败", e);
        }
    }

    @Override
    public JwtToken generateRefreshToken(
            UserId userId,
            UserDomain userDomain,
            String username,
            LoginContext loginContext
    ) {
        TokenId tokenId = TokenId.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshTokenTtlSeconds);

        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .jwtID(tokenId.value())
                    .subject(userId.value())
                    .claim("userDomain", userDomain.name())
                    .claim("username", username)
                    .claim("tokenType", "REFRESH")
                    .claim("deviceId", loginContext.deviceId().value())
                    .claim("ip", loginContext.ip().value())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claims
            );
            signedJWT.sign(new RSASSASigner(privateKey));

            String rawToken = signedJWT.serialize();

            return new JwtToken(
                    tokenId,
                    userId,
                    userDomain,
                    TokenType.REFRESH_TOKEN,
                    rawToken,
                    Collections.emptySet(),
                    loginContext,
                    now.getEpochSecond(),
                    expiresAt.getEpochSecond(),
                    now.getEpochSecond()
            );

        } catch (JOSEException e) {
            throw new TokenGenerationException("生成Refresh Token失败", e);
        }
    }

    @Override
    public Optional<JwtToken> verifyToken(String rawToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(rawToken);

            // 验证签名
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 验证过期时间
            if (claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }

            return Optional.of(parseJwtToken(rawToken, claims));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<JwtToken> parseToken(String rawToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(rawToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return Optional.of(parseJwtToken(rawToken, claims));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public JwtToken refreshAccessToken(JwtToken refreshToken, Set<String> authorities) {
        if (refreshToken.type() != TokenType.REFRESH_TOKEN) {
            throw new IllegalArgumentException("只能使用RefreshToken刷新");
        }

        if (refreshToken.isExpired(Instant.now().getEpochSecond())) {
            throw new TokenExpiredException("RefreshToken已过期");
        }

        return generateAccessToken(
                refreshToken.userId(),
                refreshToken.userDomain(),
                refreshToken.loginContext().username(),
                authorities,
                refreshToken.loginContext()
        );
    }

    @Override
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Override
    public void rotateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair newKeyPair = keyGen.generateKeyPair();
            // 这里应该实现密钥轮换逻辑
            // 需要考虑旧Token的兼容性
        } catch (NoSuchAlgorithmException e) {
            throw new KeyRotationException("密钥轮换失败", e);
        }
    }

    // ========== 私有方法 ==========

    private JwtToken parseJwtToken(String rawToken, JWTClaimsSet claims) throws java.text.ParseException {
        TokenId tokenId = new TokenId(claims.getJWTID());
        UserId userId = new UserId(claims.getSubject());
        UserDomain userDomain = UserDomain.valueOf(claims.getStringClaim("userDomain"));
        String username = claims.getStringClaim("username");

        @SuppressWarnings("unchecked")
        List<String> authList = (List<String>) claims.getClaim("authorities");
        Set<String> authorities = authList != null ? new HashSet<>(authList) : Collections.emptySet();

        DeviceId deviceId = new DeviceId(claims.getStringClaim("deviceId"));
        IpAddress ip = new IpAddress(claims.getStringClaim("ip"));
        LoginContext loginContext = new LoginContext(deviceId, ip, username);

        TokenType tokenType = "REFRESH".equals(claims.getStringClaim("tokenType"))
                ? TokenType.REFRESH_TOKEN
                : TokenType.ACCESS_TOKEN;

        return new JwtToken(
                tokenId,
                userId,
                userDomain,
                tokenType,
                rawToken,
                authorities,
                loginContext,
                claims.getIssueTime().getTime() / 1000,
                claims.getExpirationTime().getTime() / 1000,
                claims.getIssueTime().getTime() / 1000
        );
    }
}
```

#### 6.3.3 ShiroJwtAdapter 实现 (预留)

```java
package com.pot.auth.infrastructure.adapter.token;

import com.pot.auth.domain.port.TokenManagementPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Apache Shiro JWT适配器 (预留)
 * 将TokenManagementPort接口适配到Apache Shiro的JWT实现
 */
@Component
@ConditionalOnProperty(name = "auth.token.provider", havingValue = "shiro")
public class ShiroJwtAdapter implements TokenManagementPort {

    // ... 使用Shiro实现JWT管理

    // 提示: Shiro的实现方式与Spring Security不同
    // 但对于领域层来说，两者没有区别，都是通过TokenManagementPort接口访问
}
```

---

### 6.4 通知防腐层 (Notification Anti-Corruption Layer)

#### 6.4.1 NotificationPort 接口定义

```java
// 端口接口 - 放在 domain/port 包
package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.*;

/**
 * 通知端口接口
 * 领域层通过此接口发送通知，不依赖具体的通知服务
 */
public interface NotificationPort {

    /**
     * 发送短信验证码
     */
    void sendSmsCode(PhoneNumber phone, VerificationCode code);

    /**
     * 发送邮件验证码
     */
    void sendEmailCode(Email email, VerificationCode code);

    /**
     * 发送登录通知
     */
    void sendLoginNotification(UserId userId, LoginContext loginContext);

    /**
     * 发送异地登录警告
     */
    void sendAbnormalLoginWarning(UserId userId, IpAddress ip);

    /**
     * 发送密码重置通知
     */
    void sendPasswordResetNotification(UserId userId);

    /**
     * 发送设备踢出通知
     */
    void sendDeviceKickedNotification(UserId userId, DeviceId deviceId);
}
```

#### 6.4.2 CompositeNotificationAdapter 组合通知适配器

```java
// 适配器实现 - 放在 infrastructure/adapter/notification 包
package com.pot.auth.infrastructure.adapter.notification;

import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.shared.valueobject.*;
import org.springframework.stereotype.Component;

/**
 * 组合通知适配器
 * 整合SMS、Email、Push等多种通知渠道
 */
@Component
public class CompositeNotificationAdapter implements NotificationPort {

    private final SmsNotificationAdapter smsAdapter;
    private final EmailNotificationAdapter emailAdapter;
    private final PushNotificationAdapter pushAdapter;

    public CompositeNotificationAdapter(
            SmsNotificationAdapter smsAdapter,
            EmailNotificationAdapter emailAdapter,
            PushNotificationAdapter pushAdapter
    ) {
        this.smsAdapter = smsAdapter;
        this.emailAdapter = emailAdapter;
        this.pushAdapter = pushAdapter;
    }

    @Override
    public void sendSmsCode(PhoneNumber phone, VerificationCode code) {
        smsAdapter.sendCode(phone, code);
    }

    @Override
    public void sendEmailCode(Email email, VerificationCode code) {
        emailAdapter.sendCode(email, code);
    }

    @Override
    public void sendLoginNotification(UserId userId, LoginContext loginContext) {
        // 同时发送Push和Email通知
        pushAdapter.sendLoginNotification(userId, loginContext);
        // emailAdapter.sendLoginNotification(userId, loginContext);  // 可选
    }

    @Override
    public void sendAbnormalLoginWarning(UserId userId, IpAddress ip) {
        // 异地登录警告，使用多渠道通知
        smsAdapter.sendAbnormalLoginWarning(userId, ip);
        pushAdapter.sendAbnormalLoginWarning(userId, ip);
        emailAdapter.sendAbnormalLoginWarning(userId, ip);
    }

    @Override
    public void sendPasswordResetNotification(UserId userId) {
        emailAdapter.sendPasswordResetNotification(userId);
        pushAdapter.sendPasswordResetNotification(userId);
    }

    @Override
    public void sendDeviceKickedNotification(UserId userId, DeviceId deviceId) {
        pushAdapter.sendDeviceKickedNotification(userId, deviceId);
    }
}
```

#### 6.4.3 SmsNotificationAdapter 短信通知适配器

```java
package com.pot.auth.infrastructure.adapter.notification;

import com.pot.auth.domain.shared.valueobject.*;
import com.pot.touch.sms.SmsClient;  // framework-starter-touch
import org.springframework.stereotype.Component;

/**
 * 短信通知适配器
 * 适配到阿里云短信或framework-starter-touch
 */
@Component
public class SmsNotificationAdapter {

    private final SmsClient smsClient;

    public SmsNotificationAdapter(SmsClient smsClient) {
        this.smsClient = smsClient;
    }

    public void sendCode(PhoneNumber phone, VerificationCode code) {
        smsClient.sendTemplate(
                phone.value(),
                "SMS_VERIFICATION_CODE",
                Map.of("code", code.value())
        );
    }

    public void sendAbnormalLoginWarning(UserId userId, IpAddress ip) {
        // 实现异地登录短信通知
        smsClient.sendTemplate(
                getUserPhone(userId),
                "SMS_ABNORMAL_LOGIN",
                Map.of("ip", ip.value())
        );
    }

    private String getUserPhone(UserId userId) {
        // 从member-service获取用户手机号
        return null;  // 实现省略
    }
}
```

---

### 6.5 分布式锁防腐层 (Distributed Lock Anti-Corruption Layer)

#### 6.5.1 DistributedLockPort 接口定义

```java
// 端口接口 - 放在 domain/port 包
package com.pot.auth.domain.port;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁端口接口
 * 领域层通过此接口使用分布式锁，不依赖具体实现(Redisson/Zookeeper)
 */
public interface DistributedLockPort {

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的Key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime);

    /**
     * 释放锁
     */
    void unlock(String lockKey);

    /**
     * 执行带锁的操作
     *
     * @param lockKey 锁的Key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param action 需要执行的操作
     * @return 操作结果
     */
    <T> T executeWithLock(
            String lockKey,
            Duration waitTime,
            Duration leaseTime,
            Supplier<T> action
    );

    /**
     * 执行带锁的操作 (无返回值)
     */
    void executeWithLock(
            String lockKey,
            Duration waitTime,
            Duration leaseTime,
            Runnable action
    );
}
```

#### 6.5.2 RedisDistributedLockAdapter 实现

```java
// 适配器实现 - 放在 infrastructure/adapter/lock 包
package com.pot.auth.infrastructure.adapter.lock;

import com.pot.auth.domain.port.DistributedLockPort;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis分布式锁适配器
 * 基于Redisson实现
 */
@Component
@ConditionalOnProperty(name = "auth.lock.type", havingValue = "redis", matchIfMissing = true)
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;

    public RedisDistributedLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <T> T executeWithLock(
            String lockKey,
            Duration waitTime,
            Duration leaseTime,
            Supplier<T> action
    ) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockAcquisitionException("无法获取锁: " + lockKey);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("获取锁时被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void executeWithLock(
            String lockKey,
            Duration waitTime,
            Duration leaseTime,
            Runnable action
    ) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            action.run();
            return null;
        });
    }
}
```

---

### 6.6 防腐层架构总结

#### 6.6.1 端口接口清单 (领域层定义)

```
com.pot.auth.domain.port/
├── CachePort.java                    // 缓存端口
├── UserModulePort.java               // 用户模块端口
├── UserModulePortFactory.java        // 用户模块端口工厂
├── TokenManagementPort.java          // Token管理端口
├── NotificationPort.java             // 通知端口
└── DistributedLockPort.java          // 分布式锁端口
```

#### 6.6.2 适配器实现清单 (基础设施层)

```
com.pot.auth.infrastructure.adapter/
├── cache/
│   ├── RedisCacheAdapter.java         // Redis缓存适配器
│   ├── LocalCacheAdapter.java         // 本地缓存适配器 (Caffeine)
│   └── CompositeCacheAdapter.java     // 组合缓存适配器 (L1+L2)
│
├── usermodule/
│   ├── MemberModuleAdapter.java       // Member域适配器
│   └── AdminModuleAdapter.java        // Admin域适配器 (预留)
│
├── token/
│   ├── SpringSecurityJwtAdapter.java  // Spring Security JWT适配器
│   └── ShiroJwtAdapter.java           // Shiro JWT适配器 (预留)
│
├── notification/
│   ├── CompositeNotificationAdapter.java  // 组合通知适配器
│   ├── SmsNotificationAdapter.java        // 短信通知适配器
│   ├── EmailNotificationAdapter.java      // 邮件通知适配器
│   └── PushNotificationAdapter.java       // 推送通知适配器
│
└── lock/
    ├── RedisDistributedLockAdapter.java   // Redis分布式锁适配器
    └── LocalLockAdapter.java              // 本地锁适配器 (测试用)
```

#### 6.6.3 技术选型配置

```yaml
# application.yml

auth:
  # 缓存实现选择
  cache:
    type: redis  # redis | local | composite

  # Token管理实现选择
  token:
    provider: spring-security  # spring-security | shiro

  # 分布式锁实现选择
  lock:
    type: redis  # redis | local

  # 用户域启用配置
  user-domain:
    member:
      enabled: true
    admin:
      enabled: false  # 暂未启用
```

#### 6.6.4 防腐层的价值

| 防腐层                     | 解耦的具体技术                    | 扩展场景                              |
|-------------------------|----------------------------|-----------------------------------|
| **CachePort**           | Redis / Caffeine           | 从Redis切换到本地缓存，或使用L1+L2组合缓存        |
| **UserModulePort**      | OpenFeign / Member-Service | 接入新用户域(Admin/Merchant)，或更换RPC框架   |
| **TokenManagementPort** | Spring Security / Shiro    | 从Spring Security切换到Shiro，或自研JWT方案 |
| **NotificationPort**    | 阿里云短信 / 腾讯短信               | 切换短信供应商，或增加新通知渠道                  |
| **DistributedLockPort** | Redisson / Zookeeper       | 从Redis锁切换到Zookeeper锁              |

#### 6.6.5 防腐层使用示例

```java

@Service
public class AuthenticationDomainService {

    // ✅ 领域服务只依赖端口接口，完全不知道底层使用了什么技术
    private final CachePort cachePort;
    private final UserModulePortFactory userModulePortFactory;
    private final TokenManagementPort tokenPort;
    private final NotificationPort notificationPort;
    private final DistributedLockPort lockPort;

    public AuthenticationResult login(
            String identifier,
            String password,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        String lockKey = "auth:login:" + identifier;

        return lockPort.executeWithLock(lockKey, Duration.ofSeconds(3), Duration.ofSeconds(10), () -> {
            // 1. 获取对应域的用户模块端口
            UserModulePort userPort = userModulePortFactory.getPort(userDomain);

            // 2. 认证用户
            UserDTO user = userPort.authenticateWithPassword(identifier, password)
                    .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

            // 3. 查询权限
            Set<String> permissions = userPort.getPermissions(user.userId());

            // 4. 生成Token
            JwtToken accessToken = tokenPort.generateAccessToken(
                    user.userId(),
                    userDomain,
                    user.username(),
                    permissions,
                    loginContext
            );

            JwtToken refreshToken = tokenPort.generateRefreshToken(
                    user.userId(),
                    userDomain,
                    user.username(),
                    loginContext
            );

            // 5. 缓存权限
            cachePort.set(
                    "auth:permissions:" + user.userId().value() + ":" + userDomain,
                    permissions,
                    Duration.ofMinutes(60)
            );

            // 6. 发送登录通知
            notificationPort.sendLoginNotification(user.userId(), loginContext);

            return new AuthenticationResult(accessToken, refreshToken, permissions);
        });
    }
}

// 优势说明:
// 1. ✅ 领域服务代码不需要知道使用的是Redis还是Caffeine
// 2. ✅ 领域服务代码不需要知道使用的是Spring Security还是Shiro
// 3. ✅ 领域服务代码不需要知道使用的是阿里云短信还是腾讯短信
// 4. ✅ 领域服务代码不需要知道使用的是Redisson还是Zookeeper
// 5. ✅ 所有这些技术选型都可以通过配置文件切换，无需修改业务代码
// 6. ✅ 单元测试时可以使用Mock实现，无需启动真实的Redis/数据库
```

---

### 6.7 Feign Client定义 (基础设施层)

#### 6.7.1 MemberServiceClient (Feign)

```java
// Feign Client - 放在 infrastructure/client 包
package com.pot.auth.infrastructure.client;

// ✅ 使用member-facade的DTO

import com.pot.member.facade.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Member服务的Feign Client
 *
 * 依赖关系：
 * 1. auth-service依赖member-facade.jar
 * 2. 使用member-facade定义的DTO
 * 3. member-service实现member-facade的接口
 * 4. MemberModuleAdapter负责将facade的DTO转换成auth领域层的DTO (防腐层)
 */
@FeignClient(name = "member-service", path = "/internal/member")
public interface MemberServiceClient {

    // ========== 认证相关 ==========

    @PostMapping("/authenticate")
    AuthenticateResponse authenticate(@RequestBody AuthenticateRequest request);

    @GetMapping("/{userId}")
    MemberDTO getById(@PathVariable String userId);

    @GetMapping("/by-identifier")
    MemberDTO getByIdentifier(@RequestParam String identifier);

    // ========== 注册相关 ==========

    @PostMapping("/create")
    CreateMemberResponse create(@RequestBody CreateMemberRequest request);

    @GetMapping("/exists")
    boolean exists(@RequestParam String field, @RequestParam String value);

    // ========== 密码管理 ==========

    @PutMapping("/{userId}/password")
    void updatePassword(
            @PathVariable String userId,
            @RequestBody UpdatePasswordRequest request
    );

    // ========== 账户管理 ==========

    @PutMapping("/{userId}/lock")
    void lockAccount(@PathVariable String userId);

    @PutMapping("/{userId}/unlock")
    void unlockAccount(@PathVariable String userId);

    @PostMapping("/{userId}/login-attempt")
    void recordLoginAttempt(
            @PathVariable String userId,
            @RequestBody LoginAttemptRequest request
    );

    // ========== 权限查询 ==========

    @GetMapping("/{userId}/permissions")
    Set<String> getPermissions(@PathVariable String userId);

    @GetMapping("/{userId}/roles")
    Set<RoleDTO> getRoles(@PathVariable String userId);

    @PostMapping("/permissions/batch")
    Map<String, Set<String>> getPermissionsBatch(@RequestBody List<String> userIds);

    // ========== 设备管理 ==========

    @GetMapping("/{userId}/devices")
    List<DeviceDTO> getDevices(@PathVariable String userId);

    @PostMapping("/{userId}/devices")
    void recordDeviceLogin(
            @PathVariable String userId,
            @RequestBody DeviceLoginRequest request
    );

    @DeleteMapping("/{userId}/devices/{deviceId}")
    void kickDevice(
            @PathVariable String userId,
            @PathVariable String deviceId
    );

    // ========== OAuth2绑定 ==========

    @GetMapping("/oauth2/{provider}/{providerId}")
    String findUserIdByOAuth2(
            @PathVariable String provider,
            @PathVariable String providerId
    );

    @PostMapping("/{userId}/oauth2")
    void bindOAuth2(
            @PathVariable String userId,
            @RequestBody BindOAuth2Request request
    );
}

public interface MemberServiceClient {

    // ========== 认证相关 ==========

    @PostMapping("/authenticate")
    MemberDTO authenticate(@RequestBody AuthenticateRequest request);

    @GetMapping("/{userId}")
    MemberDTO getById(@PathVariable String userId);

    @GetMapping("/by-identifier")
    MemberDTO getByIdentifier(@RequestParam String identifier);

    // ========== 注册相关 ==========

    @PostMapping("/create")
    CreateMemberResponse create(@RequestBody CreateMemberRequest request);

    @GetMapping("/exists")
    boolean exists(@RequestParam String field, @RequestParam String value);

    // ========== 密码管理 ==========

    @PutMapping("/{userId}/password")
    void updatePassword(
            @PathVariable String userId,
            @RequestBody UpdatePasswordRequest request
    );

    // ========== 账户管理 ==========

    @PutMapping("/{userId}/lock")
    void lockAccount(@PathVariable String userId);

    @PutMapping("/{userId}/unlock")
    void unlockAccount(@PathVariable String userId);

    @PostMapping("/{userId}/login-attempt")
    void recordLoginAttempt(
            @PathVariable String userId,
            @RequestBody LoginAttemptRequest request
    );

    // ========== 权限查询 ==========

    @GetMapping("/{userId}/permissions")
    Set<String> getPermissions(@PathVariable String userId);

    @GetMapping("/{userId}/roles")
    Set<RoleDTO> getRoles(@PathVariable String userId);

    @PostMapping("/permissions/batch")
    Map<String, Set<String>> getPermissionsBatch(@RequestBody List<String> userIds);

    // ========== 设备管理 ==========

    @GetMapping("/{userId}/devices")
    List<DeviceDTO> getDevices(@PathVariable String userId);

    @PostMapping("/{userId}/devices")
    void recordDeviceLogin(
            @PathVariable String userId,
            @RequestBody DeviceLoginRequest request
    );

    @DeleteMapping("/{userId}/devices/{deviceId}")
    void kickDevice(
            @PathVariable String userId,
            @PathVariable String deviceId
    );

    // ========== OAuth2绑定 ==========

    @GetMapping("/oauth2/{provider}/{providerId}")
    String findUserIdByOAuth2(
            @PathVariable String provider,
            @PathVariable String providerId
    );

    @PostMapping("/{userId}/oauth2")
    void bindOAuth2(
            @PathVariable String userId,
            @RequestBody BindOAuth2Request request
    );
}
```

#### 6.7.2 AdminServiceClient (Feign - 预留)

```java
package com.pot.auth.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Admin服务的Feign Client (预留)
 * 定义auth-service调用admin-service的内部API
 */
@FeignClient(name = "admin-service", path = "/internal/admin")
public interface AdminServiceClient {
    // 与MemberServiceClient类似的接口定义
    // 但可能有Admin域特有的API (如组织架构、权限管理等)
}
```

---

### 6.8 防腐层设计总结

#### 6.8.1 设计原则总结

1. **依赖倒置原则 (DIP)**
    - 领域层定义端口接口（Port Interface）
    - 基础设施层实现适配器（Adapter Implementation）
    - 高层模块（领域层）不依赖低层模块（基础设施层）

2. **开闭原则 (OCP)**
    - 对扩展开放：新增缓存实现、新增用户域，只需添加新的适配器
    - 对修改关闭：领域层代码无需修改

3. **单一职责原则 (SRP)**
    - 每个端口接口只负责一个职责
    - 每个适配器只适配一个具体技术

4. **接口隔离原则 (ISP)**
    - 端口接口粒度合理，不强迫依赖者依赖不需要的方法

#### 6.8.2 防腐层带来的收益

| 收益维度       | 说明                     | 示例                                        |
|------------|------------------------|-------------------------------------------|
| **技术选型灵活** | 可以无缝切换底层技术实现           | Redis → Caffeine, Spring Security → Shiro |
| **测试友好**   | 领域层可以使用Mock测试，无需启动真实依赖 | 使用MockCachePort测试业务逻辑                     |
| **领域纯粹**   | 领域层代码不包含任何技术细节         | 无Redis/Feign/Spring Security代码            |
| **演进独立**   | 领域层与基础设施层可以独立演进        | 升级Spring Boot版本不影响领域代码                    |
| **多实现并存**  | 可以同时支持多种实现，运行时动态选择     | 组合缓存(L1本地+L2 Redis)                       |

#### 6.8.3 防腐层架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层 (Application)                       │
│                      LoginApplicationService                      │
│                   RegistrationApplicationService                  │
│                      PermissionQueryService                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │ 调用
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        领域层 (Domain)                            │
│                   AuthenticationDomainService                     │
│                   RegistrationOrchestrationService                │
│                                                                   │
│  依赖抽象 ▼                                                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 端口接口 (Port Interfaces)                                │   │
│  │ - CachePort                                               │   │
│  │ - UserModulePort                                          │   │
│  │ - TokenManagementPort                                     │   │
│  │ - NotificationPort                                        │   │
│  │ - DistributedLockPort                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────┘
                                │ 实现
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)                    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 适配器实现 (Adapter Implementations)                      │   │
│  │                                                            │   │
│  │ RedisCacheAdapter           → RedisTemplate              │   │
│  │ LocalCacheAdapter            → Caffeine                   │   │
│  │ MemberModuleAdapter          → MemberServiceClient        │   │
│  │ SpringSecurityJwtAdapter     → Spring Security OAuth2     │   │
│  │ CompositeNotificationAdapter → SMS/Email/Push            │   │
│  │ RedisDistributedLockAdapter  → Redisson                   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 外部依赖 (External Dependencies)                          │   │
│  │ - Spring Data Redis                                       │   │
│  │ - OpenFeign                                               │   │
│  │ - Spring Security OAuth2                                  │   │
│  │ - Redisson                                                │   │
│  │ - framework-starter-touch                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

#### 6.8.4 关键要点

1. **领域层完全不依赖具体技术框架**
    - ✅ 无`@Autowired RedisTemplate`
    - ✅ 无`@Autowired MemberServiceClient`
    - ✅ 无`@Autowired JwtEncoder`
    - ✅ 只依赖端口接口 (`CachePort`, `UserModulePort`, `TokenManagementPort`)

2. **所有外部交互都通过端口接口**
    - 缓存 → `CachePort`
    - 用户模块 → `UserModulePort`
    - Token管理 → `TokenManagementPort`
    - 通知 → `NotificationPort`
    - 分布式锁 → `DistributedLockPort`

3. **适配器可以灵活切换**
    - 通过`@ConditionalOnProperty`实现配置化切换
    - 领域层代码无需任何修改

4. **测试友好**
   ```java
   @Test
   void testLogin() {
       // ✅ 使用Mock，无需启动Redis/Feign
       CachePort mockCache = Mockito.mock(CachePort.class);
       UserModulePort mockUserModule = Mockito.mock(UserModulePort.class);
       TokenManagementPort mockToken = Mockito.mock(TokenManagementPort.class);
       
       AuthenticationDomainService service = new AuthenticationDomainService(
           mockCache,
           mockUserModule,
           mockToken,
           ...
       );
       
       // 执行测试
       service.login(...);
   }
   ```

---

## 7. 核心流程设计

### 7.1 注册流程

#### 密码注册流程

```
1. POST /auth/register/password
   {
     "username": "john_doe",
     "email": "user@example.com",
     "phone": "+8613800138000",
     "password": "SecurePass123!",
     "userDomain": "MEMBER"
   }
   ↓
2. Auth-Service处理:
   ├─ 调用memberAdapter.existsByUsername() 
   ├─ 调用memberAdapter.existsByEmail()
   ├─ 调用memberAdapter.existsByPhone()
   ├─ 生成验证Token (UUID)
   ├─ 存储到Redis: auth:verification:{email} = {token, username, password...}
   └─ 调用touchService发送验证邮件
   ↓
3. 返回:
   {
     "status": "PENDING_VERIFICATION",
     "message": "请验证邮箱"
   }
   ↓
4. 用户点击邮件链接:
   POST /auth/register/verify-email
   {
     "token": "verification-token-xxx"
   }
   ↓
5. Auth-Service处理:
   ├─ 从Redis读取注册数据
   ├─ 调用memberAdapter.createUser() 创建用户
   │  └─ member-service在member_db创建用户
   ├─ 返回userId
   ├─ 删除Redis验证数据
   └─ 可选: 自动登录返回Token
   ↓
6. 注册完成
```

#### 验证码注册流程 (自动登录)

```
1. POST /auth/code/send
   {
     "type": "EMAIL",
     "recipient": "user@example.com",
     "purpose": "REGISTER"
   }
   ↓
2. Auth-Service处理:
   ├─ 生成6位验证码
   ├─ 存储到Redis: auth:code:REGISTER:user@example.com
   │  TTL=5分钟, attempts=0
   └─ 调用touchService发送验证码
   ↓
3. POST /auth/register/code
   {
     "recipient": "user@example.com",
     "code": "123456",
     "userDomain": "MEMBER"
   }
   ↓
4. Auth-Service处理:
   ├─ 从Redis验证验证码
   ├─ 调用memberAdapter.existsByEmail()
   ├─ 自动生成username (user_1699516800)
   ├─ 调用memberAdapter.createUser() 创建用户
   ├─ 生成JWT Token
   └─ 记录设备登录
   ↓
5. 返回Token (自动登录):
   {
     "userId": "user-123",
     "username": "user_1699516800",
     "accessToken": "...",
     "refreshToken": "..."
   }
```

### 7.2 登录流程

#### 密码登录流程

```
1. POST /auth/login/password
   {
     "identifier": "john_doe",
     "password": "SecurePass123!",
     "userDomain": "MEMBER",
     "deviceInfo": {
       "deviceId": "uuid-xxx",
       "deviceType": "WEB",
       "deviceName": "Chrome on macOS"
     },
     "ipAddress": "123.456.789.0"
   }
   ↓
2. Auth-Service处理:
   ├─ 调用memberAdapter.authenticateWithPassword()
   │  └─ member-service验证密码 (BCrypt)
   │  └─ 返回UserDTO
   ├─ 调用memberAdapter.getPermissions()
   │  └─ member-service查询权限
   ├─ 生成JWT Token
   │  Payload: {userId, userDomain, username, authorities}
   ├─ 存储RefreshToken到Redis
   │  Key: auth:refresh:{jti}
   │  Value: {userId, userDomain, deviceId}
   │  TTL: 30天
   ├─ 调用memberAdapter.recordDeviceLogin()
   │  └─ member-service记录到member_device表
   └─ 检查异地登录
   ↓
3. 返回Token:
   {
     "userId": "user-123",
     "username": "john_doe",
     "accessToken": "eyJhbGc...",
     "refreshToken": "eyJhbGc...",
     "accessTokenExpiresAt": 1699520400,
     "refreshTokenExpiresAt": 1702108800,
     "isSuspiciousLogin": false
   }
```

### 7.3 Token刷新流程 (含自动续期)

```
1. POST /auth/refresh
   {
     "refreshToken": "eyJhbGc..."
   }
   ↓
2. Auth-Service处理:
   ├─ 验证RefreshToken签名
   ├─ 检查黑名单 (Redis)
   ├─ 从Token解析userId, userDomain
   ├─ 调用memberAdapter.getPermissions() 获取最新权限
   ├─ 生成新AccessToken
   ├─ 判断是否续期RefreshToken
   │  └─ 距上次刷新 < 7天 → 生成新RefreshToken
   ├─ 更新Redis中的RefreshToken记录
   └─ 如果续期，旧RefreshToken加入黑名单
   ↓
3. 返回:
   {
     "accessToken": "new_access_token",
     "refreshToken": "new_refresh_token",  // 可能是新的
     "accessTokenExpiresAt": 1699524000,
     "refreshTokenExpiresAt": 1702195200,
     "refreshTokenRenewed": true
   }
```

### 7.4 权限查询流程

```
1. Backend Service调用:
   GET /auth/permissions/check
   {
     "userId": "user-123",
     "userDomain": "MEMBER",
     "permission": "user:delete"
   }
   ↓
2. Auth-Service处理:
   ├─ 检查Redis缓存
   │  Key: auth:permissions:user-123:MEMBER
   ├─ 缓存命中 → 直接返回
   ├─ 缓存未命中:
   │  ├─ 调用memberAdapter.getPermissions()
   │  ├─ member-service查询:
   │  │  member_member_role → member_role_permission → member_permission
   │  ├─ 缓存到Redis (TTL=60秒)
   │  └─ 返回权限集合
   └─ 检查是否包含指定权限
   ↓
3. 返回:
   {
     "hasPermission": true
   }
```

### 7.5 设备踢出流程

```
1. POST /auth/devices/{deviceId}/kick
   ↓
2. Auth-Service处理:
   ├─ 调用memberAdapter.getDevices() 获取设备列表
   ├─ 找到目标设备的refreshToken
   ├─ 将refreshToken加入黑名单 (Redis)
   │  Key: auth:blacklist:{jti}
   │  Reason: DEVICE_KICKED
   ├─ 发布Redis Pub/Sub事件
   │  Channel: auth:token:revoked
   │  Message: {jti}
   ├─ 调用memberAdapter.kickDevice()
   │  └─ member-service更新member_device状态
   └─ Gateway订阅事件，清除本地缓存
   ↓
3. 该设备后续请求返回401
```

---

## 8. 技术栈选型

### 8.1 核心技术栈

| 类别     | 技术                            | 版本    | 说明             |
|--------|-------------------------------|-------|----------------|
| 框架     | Spring Boot                   | 3.2+  |                |
| 安全     | Spring Security               | 6.2+  | JWT无状态         |
| JWT    | jjwt                          | 0.12+ | RSA-256        |
| 缓存     | Redis                         | 7.0+  | Lettuce, 唯一存储  |
| 服务调用   | OpenFeign                     | 4.x   | 调用member/admin |
| 注册中心   | Nacos                         | 2.3+  | 服务发现           |
| OAuth2 | Spring Security OAuth2 Client | -     | GitHub/Google  |
| 微信     | weixin-java-open              | 4.6+  | 微信开放平台         |
| 消息     | Redis Pub/Sub                 | -     | Token黑名单同步     |

### 8.2 不使用的技术

| 技术                   | 原因                 |
|----------------------|--------------------|
| MySQL                | ❌ Auth-Service无数据库 |
| MyBatis/MyBatis Plus | ❌ 无需ORM            |
| Flyway               | ❌ 无数据库迁移           |
| JPA/Hibernate        | ❌ 无需ORM            |

---

## 9. 目录结构设计

### 9.1 pom.xml依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.pot</groupId>
    <artifactId>auth-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Nacos服务发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Redisson (分布式锁) -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>

        <!-- Caffeine (本地缓存) -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
        </dependency>

        <!-- ⭐⭐⭐ 依赖member-facade (API契约) -->
        <dependency>
            <groupId>com.pot</groupId>
            <artifactId>member-facade</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- ⭐⭐⭐ 依赖admin-facade (API契约 - 预留) -->
        <dependency>
            <groupId>com.pot</groupId>
            <artifactId>admin-facade</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <optional>true</optional>
        </dependency>

        <!-- 自定义框架 -->
        <dependency>
            <groupId>com.pot</groupId>
            <artifactId>framework-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.pot</groupId>
            <artifactId>framework-starter-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.pot</groupId>
            <artifactId>framework-starter-touch</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**依赖说明**：

1. ✅ **member-facade**: auth-service依赖member-facade.jar获取API契约和DTO定义
2. ✅ **admin-facade**: auth-service依赖admin-facade.jar（预留，暂未启用）
3. ✅ **不依赖member-service**: auth-service不依赖member-service的实现jar包
4. ✅ **防腐层隔离**: MemberModuleAdapter负责将facade DTO转换成auth领域DTO

### 9.2 目录结构

```
auth-service/
├── pom.xml                               ← 依赖member-facade.jar
├── DDD-REFACTORING-SPEC.md
├── src/
│   ├── main/
│   │   ├── java/com/pot/auth/
│   │   │   ├── AuthServiceApplication.java
│   │   │   │
│   │   │   ├── interfaces/                    # 接口层
│   │   │   │   ├── rest/
│   │   │   │   │   ├── AuthenticationController.java
│   │   │   │   │   ├── RegistrationController.java
│   │   │   │   │   ├── PermissionController.java
│   │   │   │   │   └── DeviceController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   ├── converter/
│   │   │   │
│   │   │   ├── application/                   # 应用层
│   │   │   │   ├── service/
│   │   │   │   │   ├── LoginApplicationService.java
│   │   │   │   │   ├── RegistrationApplicationService.java
│   │   │   │   │   ├── PermissionApplicationService.java
│   │   │   │   │   └── DeviceApplicationService.java
│   │   │   │   ├── command/
│   │   │   │   └── query/
│   │   │   │
│   │   │   ├── domain/                        # 领域层 ⭐核心 (纯业务逻辑)
│   │   │   │   │
│   │   │   │   ├── port/                      # 端口接口层 (领域层定义) ⭐⭐⭐
│   │   │   │   │   ├── CachePort.java                    // 缓存端口
│   │   │   │   │   ├── UserModulePort.java               // 用户模块端口
│   │   │   │   │   ├── UserModulePortFactory.java        // 用户模块端口工厂
│   │   │   │   │   ├── TokenManagementPort.java          // Token管理端口
│   │   │   │   │   ├── NotificationPort.java             // 通知端口
│   │   │   │   │   └── DistributedLockPort.java          // 分布式锁端口
│   │   │   │   │
│   │   │   │   ├── authentication/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── JwtToken.java
│   │   │   │   │   │   ├── TokenBlacklist.java
│   │   │   │   │   │   └── VerificationCode.java
│   │   │   │   │   ├── valueobject/
│   │   │   │   │   │   ├── LoginContext.java
│   │   │   │   │   │   ├── DeviceInfo.java
│   │   │   │   │   │   ├── TokenPair.java
│   │   │   │   │   │   ├── TokenType.java
│   │   │   │   │   │   └── AuthenticationResult.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── AuthenticationDomainService.java
│   │   │   │   │   │   ├── AuthenticationDomainServiceImpl.java
│   │   │   │   │   │   ├── JwtTokenDomainService.java
│   │   │   │   │   │   ├── JwtTokenDomainServiceImpl.java
│   │   │   │   │   │   ├── VerificationCodeService.java
│   │   │   │   │   │   └── VerificationCodeServiceImpl.java
│   │   │   │   │   └── exception/
│   │   │   │   │       ├── BadCredentialsException.java
│   │   │   │   │       ├── TokenExpiredException.java
│   │   │   │   │       └── InvalidVerificationCodeException.java
│   │   │   │   │
│   │   │   │   ├── registration/
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── RegistrationOrchestrationService.java
│   │   │   │   │   │   └── RegistrationOrchestrationServiceImpl.java
│   │   │   │   │   └── exception/
│   │   │   │   │       ├── DuplicateUsernameException.java
│   │   │   │   │       └── DuplicateEmailException.java
│   │   │   │   │
│   │   │   │   ├── permission/
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── PermissionQueryService.java
│   │   │   │   │   │   └── PermissionQueryServiceImpl.java
│   │   │   │   │   └── exception/
│   │   │   │   │
│   │   │   │   ├── device/
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── DeviceQueryService.java
│   │   │   │   │   │   └── DeviceKickService.java
│   │   │   │   │   └── exception/
│   │   │   │   │
│   │   │   │   └── shared/
│   │   │   │       ├── valueobject/               # Domain Primitive
│   │   │   │       │   ├── Email.java             // 邮箱 (有验证规则)
│   │   │   │       │   ├── PhoneNumber.java       // 手机号 (有验证规则)
│   │   │   │       │   ├── Password.java          // 密码 (有复杂度规则)
│   │   │   │       │   ├── IpAddress.java         // IP地址 (有验证 + 异地检测)
│   │   │   │       │   ├── VerificationCode.java  // 验证码 (有格式验证)
│   │   │   │       │   ├── TokenId.java           // Token ID (类型安全)
│   │   │   │       │   ├── UserId.java            // 用户ID (类型安全)
│   │   │   │       │   ├── DeviceId.java          // 设备ID (类型安全)
│   │   │   │       │   ├── Username.java          // 用户名 (有验证规则)
│   │   │   │       │   └── UserDomain.java        // 用户域枚举
│   │   │   │       ├── event/
│   │   │   │       │   ├── DomainEvent.java
│   │   │   │       │   ├── UserLoggedInEvent.java
│   │   │   │       │   └── PermissionChangedEvent.java
│   │   │   │       └── exception/
│   │   │   │           └── DomainException.java
│   │   │   │
│   │   │   └── infrastructure/                # 基础设施层 (技术实现)
│   │   │       │
│   │   │       ├── adapter/                   # 适配器实现层 ⭐⭐⭐
│   │   │       │   │
│   │   │       │   ├── cache/                 # 缓存适配器
│   │   │       │   │   ├── RedisCacheAdapter.java        // Redis缓存实现
│   │   │       │   │   ├── LocalCacheAdapter.java        // 本地缓存实现 (Caffeine)
│   │   │       │   │   └── CompositeCacheAdapter.java    // 组合缓存 (L1+L2)
│   │   │       │   │
│   │   │       │   ├── usermodule/            # 用户模块适配器
│   │   │       │   │   ├── MemberModuleAdapter.java      // Member域适配器
│   │   │       │   │   └── AdminModuleAdapter.java       // Admin域适配器 (预留)
│   │   │       │   │
│   │   │       │   ├── token/                 # Token管理适配器
│   │   │       │   │   ├── SpringSecurityJwtAdapter.java // Spring Security实现
│   │   │       │   │   └── ShiroJwtAdapter.java          // Shiro实现 (预留)
│   │   │       │   │
│   │   │       │   ├── notification/          # 通知适配器
│   │   │       │   │   ├── CompositeNotificationAdapter.java  // 组合通知
│   │   │       │   │   ├── SmsNotificationAdapter.java        // 短信通知
│   │   │       │   │   ├── EmailNotificationAdapter.java      // 邮件通知
│   │   │       │   │   └── PushNotificationAdapter.java       // 推送通知
│   │   │       │   │
│   │   │       │   └── lock/                  # 分布式锁适配器
│   │   │       │       ├── RedisDistributedLockAdapter.java   // Redis锁 (Redisson)
│   │   │       │       └── LocalLockAdapter.java              // 本地锁 (测试用)
│   │   │       │
│   │   │       ├── client/                    # Feign Client定义
│   │   │       │   ├── MemberServiceClient.java       // 使用member-facade的接口
│   │   │       │   └── AdminServiceClient.java        // 使用admin-facade的接口 (预留)
│   │   │       │
│   │   │       ├── oauth2/                    # OAuth2集成
│   │   │       │   ├── GitHubOAuth2Provider.java
│   │   │       │   ├── GoogleOAuth2Provider.java
│   │   │       │   └── OAuth2ProviderFactory.java
│   │   │       │
│   │   │       ├── wechat/                    # 微信集成
│   │   │       │   └── WechatOpenService.java
│   │   │       │
│   │   │       ├── messaging/                 # 消息发布
│   │   │       │   ├── RedisMessagePublisher.java
│   │   │       │   └── RedisMessageSubscriber.java
│   │   │       │
│   │   │       └── config/                    # 配置类
│   │   │           ├── SecurityConfig.java
│   │   │           ├── RedisConfig.java
│   │   │           ├── FeignConfig.java
│   │   │           └── JwtProperties.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── keys/
│   │           ├── jwt_private_key.pem
│   │           └── jwt_public_key.pem
│   │
│   └── test/
│       └── java/com/pot/auth/
│           ├── domain/
│           │   ├── authentication/
│           │   │   ├── JwtTokenTest.java
│           │   │   └── VerificationCodeTest.java
│           │   └── registration/
│           ├── application/
│           └── integration/
```

---

## 10. 实施路线图

### Phase 1: 准备 (1周)

**目标**: 团队准备 + 技术验证

**任务列表**:

- [ ] DDD培训 (值对象、领域服务、防腐层概念)
- [ ] Spring Security 6 + JWT配置验证
- [ ] 生成RSA密钥对
- [ ] Redis Pub/Sub验证
- [ ] 与member-service确认内部API接口定义

**验收标准**:

- [ ] 全员理解Auth-Service无数据库架构
- [ ] Spring Security + JWT配置成功
- [ ] member-service内部API接口文档完成

---

### Phase 2: 认证上下文 (3周)

**优先级**: 🔴 高

**任务列表**:

#### 2.1 领域模型

- [ ] JwtToken值对象
- [ ] TokenBlacklist值对象
- [ ] VerificationCode值对象
- [ ] LoginContext、DeviceInfo等值对象
- [ ] AuthenticationDomainService
- [ ] JwtTokenService
- [ ] VerificationCodeService

#### 2.2 基础设施层

- [ ] Redis缓存服务
    - [ ] TokenBlacklistCacheService
    - [ ] RefreshTokenCacheService
    - [ ] VerificationCodeCacheService
- [ ] JwtTokenProvider (Spring Security集成)
- [ ] SecurityConfig配置

#### 2.3 防腐层

- [ ] MemberServiceClient (Feign)
- [ ] UserModuleAdapter接口
- [ ] MemberModuleAdapter实现

#### 2.4 应用层

- [ ] LoginApplicationService
    - [ ] 密码登录
    - [ ] 验证码登录
    - [ ] OAuth2登录
    - [ ] 微信扫码登录
- [ ] TokenRefreshApplicationService

#### 2.5 接口层

- [ ] AuthenticationController
    - [ ] POST /auth/login/password
    - [ ] POST /auth/login/code
    - [ ] POST /auth/logout
    - [ ] POST /auth/refresh
    - [ ] POST /auth/validate
- [ ] OAuth2Controller
- [ ] WechatController

#### 2.6 Member-Service配套

- [ ] 内部API: POST /internal/member/authenticate
- [ ] 内部API: GET /internal/member/{userId}
- [ ] 内部API: GET /internal/member/by-identifier
- [ ] 内部API: POST /internal/member/{userId}/login-attempt
- [ ] 内部API: PUT /internal/member/{userId}/lock
- [ ] 内部API: PUT /internal/member/{userId}/unlock

**验收标准**:

- [ ] 4种登录方式全部正常
- [ ] JWT Token签发、验证、刷新正常
- [ ] RefreshToken滑动窗口续期正常
- [ ] Token黑名单功能正常
- [ ] 验证码5分钟过期、3次尝试限制生效
- [ ] 所有测试通过

---

### Phase 3: 注册编排上下文 (2周)

**优先级**: 🔴 高

**任务列表**:

#### 3.1 领域层

- [ ] RegistrationOrchestrationService

#### 3.2 应用层

- [ ] RegistrationApplicationService
    - [ ] 密码注册 + 邮箱验证
    - [ ] 验证码注册 (自动登录)
    - [ ] OAuth2注册 (自动登录)
    - [ ] 微信扫码注册 (自动登录)

#### 3.3 接口层

- [ ] RegistrationController
    - [ ] POST /auth/register/password
    - [ ] POST /auth/register/code
    - [ ] POST /auth/register/verify-email
    - [ ] POST /auth/register/verify-phone
    - [ ] POST /auth/code/send

#### 3.4 Member-Service配套

- [ ] 内部API: POST /internal/member/create
- [ ] 内部API: GET /internal/member/exists
- [ ] 内部API: POST /internal/member/{userId}/oauth2 (绑定)
- [ ] 内部API: GET /internal/member/oauth2/{provider}/{providerId}

**验收标准**:

- [ ] 密码注册 + 邮箱验证流程正常
- [ ] 验证码注册自动登录正常
- [ ] OAuth2注册自动登录正常
- [ ] 微信注册自动登录正常
- [ ] 唯一性检查生效
- [ ] 验证邮件/短信正常发送

---

### Phase 4: 权限查询上下文 (2周)

**优先级**: 🔴 高

**任务列表**:

#### 4.1 领域层

- [ ] PermissionQueryService

#### 4.2 基础设施层

- [ ] PermissionCacheService (Redis)

#### 4.3 应用层

- [ ] PermissionApplicationService

#### 4.4 接口层

- [ ] PermissionController
    - [ ] GET /auth/permissions/check
    - [ ] GET /auth/permissions/{userId}
    - [ ] DELETE /auth/permissions/cache/{userId}

#### 4.5 Member-Service配套

- [ ] 内部API: GET /internal/member/{userId}/permissions
- [ ] 内部API: GET /internal/member/{userId}/roles
- [ ] 内部API: POST /internal/member/permissions/batch

**验收标准**:

- [ ] 权限查询正常 (从member-service)
- [ ] 权限缓存命中率 >95%
- [ ] 权限缓存失效机制正常
- [ ] 批量权限查询性能达标

---

### Phase 5: 设备查询上下文 (1周)

**优先级**: 🟡 中

**任务列表**:

#### 5.1 领域层

- [ ] DeviceQueryService
- [ ] DeviceKickService

#### 5.2 应用层

- [ ] DeviceApplicationService

#### 5.3 接口层

- [ ] DeviceController
    - [ ] GET /auth/devices
    - [ ] POST /auth/devices/{deviceId}/kick

#### 5.4 Member-Service配套

- [ ] 内部API: GET /internal/member/{userId}/devices
- [ ] 内部API: POST /internal/member/{userId}/devices
- [ ] 内部API: DELETE /internal/member/{userId}/devices/{deviceId}

**验收标准**:

- [ ] 设备列表查询正常
- [ ] 设备踢出功能正常
- [ ] 踢出后Token失效

---

### Phase 6: Gateway集成 (1周)

**优先级**: 🔴 高

**任务列表**:

- [ ] Gateway JWT验证Filter
- [ ] Token黑名单检查 (Caffeine本地缓存 + Redis)
- [ ] Redis Pub/Sub订阅 (Token撤销事件)
- [ ] 限流Filter
- [ ] 配置与测试

**验收标准**:

- [ ] Gateway本地验证JWT <5ms
- [ ] 黑名单检查 <2ms
- [ ] 限流功能正常
- [ ] 无需调用auth-service完成认证

---

### Phase 7: 测试与上线 (1周)

**任务列表**:

- [ ] 单元测试 (>80%覆盖率)
- [ ] 集成测试
- [ ] 性能测试 (登录QPS >1000)
- [ ] 安全测试 (OWASP Top 10)
- [ ] 压力测试
- [ ] 灰度发布

**验收标准**:

- [ ] 所有测试通过
- [ ] 性能指标达标
- [ ] 无严重bug
- [ ] 成功灰度发布

---

### 📅 总工期

| 阶段               | 工期 | 累计  | 关键产出           |
|------------------|----|-----|----------------|
| Phase 1: 准备      | 1周 | 1周  | DDD培训、接口定义     |
| Phase 2: 认证      | 3周 | 4周  | 4种登录 + Token管理 |
| Phase 3: 注册      | 2周 | 6周  | 4种注册 + 验证      |
| Phase 4: 权限      | 2周 | 8周  | 权限查询 + 缓存      |
| Phase 5: 设备      | 1周 | 9周  | 设备查询 + 踢出      |
| Phase 6: Gateway | 1周 | 10周 | 网关集成           |
| Phase 7: 测试上线    | 1周 | 11周 | 灰度发布           |

**总工期**: 约11周 (2.5个月)

---

## 11. 风险评估与应对

### 11.1 技术风险

| 风险               | 等级   | 影响         | 应对策略                      |
|------------------|------|------------|---------------------------|
| Redis单点故障        | 🔴 高 | Token黑名单失效 | Redis Sentinel/Cluster高可用 |
| Member-Service故障 | 🔴 高 | 无法登录/注册    | 熔断降级 + 本地缓存兜底             |
| JWT密钥泄露          | 🔴 高 | Token伪造    | 定期轮换密钥 + 密钥管理系统           |
| 性能瓶颈             | 🟡 中 | 响应慢        | 性能测试先行 + 缓存策略优化           |

### 11.2 业务风险

| 风险    | 等级   | 影响   | 应对策略          |
|-------|------|------|---------------|
| 验证码轰炸 | 🟡 中 | 成本增加 | 频率限制 + 滑块验证   |
| 暴力破解  | 🟡 中 | 账户被盗 | 登录失败追踪 + 账户锁定 |
| 批量注册  | 🟡 中 | 垃圾数据 | 图形验证码 + IP限制  |

### 11.3 架构风险

| 风险        | 等级   | 影响       | 应对策略            |
|-----------|------|----------|-----------------|
| 无数据库缺陷    | 🟡 中 | 某些场景无法满足 | 设计评审 + 充分验证     |
| Feign调用失败 | 🟡 中 | 功能不可用    | 熔断降级 + 重试机制     |
| Redis数据丢失 | 🟡 中 | Token失效  | Redis持久化 + 定期备份 |

---

## 12. 验收标准

### 12.1 功能验收

#### 认证功能

- [ ] ✅ 密码登录 (用户名/邮箱/手机号)
- [ ] ✅ 验证码登录 (邮箱/手机号)
- [ ] ✅ OAuth2登录 (GitHub/Google)
- [ ] ✅ 微信扫码登录
- [ ] ✅ Token刷新 (滑动窗口续期)
- [ ] ✅ 登出 (Token黑名单)

#### 注册功能

- [ ] ✅ 密码注册 + 邮箱验证
- [ ] ✅ 验证码注册 + 自动登录
- [ ] ✅ OAuth2注册 + 自动登录
- [ ] ✅ 微信注册 + 自动登录
- [ ] ✅ 唯一性检查 (用户名/邮箱/手机号)

#### 权限功能

- [ ] ✅ 权限查询 (从member-service)
- [ ] ✅ 权限缓存 (Redis 60秒TTL)
- [ ] ✅ 权限缓存失效

#### 设备管理

- [ ] ✅ 设备列表查询
- [ ] ✅ 设备踢出
- [ ] ✅ 异地登录检测

### 12.2 架构验收

#### 防腐层验收 ⭐⭐⭐

- [ ] ✅ 领域层不依赖任何具体技术框架
    - 无 `@Autowired RedisTemplate`
    - 无 `@Autowired MemberServiceClient`
    - 无 `@Autowired JwtEncoder`
    - 只依赖端口接口 (`CachePort`, `UserModulePort`, `TokenManagementPort`)

- [ ] ✅ 所有外部交互都通过端口接口
    - 缓存 → `CachePort`
    - 用户模块 → `UserModulePort`
    - Token管理 → `TokenManagementPort`
    - 通知 → `NotificationPort`
    - 分布式锁 → `DistributedLockPort`

- [ ] ✅ 适配器可以灵活切换
    - 缓存：Redis ↔ Caffeine ↔ Composite
    - 安全框架：Spring Security ↔ Shiro
    - 通知：阿里云短信 ↔ 腾讯短信
    - 分布式锁：Redisson ↔ Zookeeper

- [ ] ✅ 配置文件控制技术选型
  ```yaml
  auth:
    cache:
      type: redis  # redis | local | composite
    token:
      provider: spring-security  # spring-security | shiro
    lock:
      type: redis  # redis | local
  ```

#### DDD架构验收

- [ ] ✅ Auth-Service无MySQL数据库
- [ ] ✅ 所有持久化数据在Member-Service
- [ ] ✅ 通过Feign调用member-service
- [ ] ✅ 防腐层隔离外部服务
- [ ] ✅ Redis存储临时数据 (有TTL)
- [ ] ✅ 领域模型充血 (包含业务逻辑)
- [ ] ✅ 值对象不可变
- [ ] ✅ Domain Primitive合理使用 (Email、PhoneNumber、Password等)

### 12.3 性能验收

| 指标           | 目标           | 测试方法      |
|--------------|--------------|-----------|
| 登录QPS        | >1000        | JMeter压测  |
| 登录响应时间       | <200ms (P99) | APM监控     |
| JWT验证        | <5ms         | Gateway监控 |
| 权限查询 (缓存命中)  | <10ms        | Redis监控   |
| 权限查询 (缓存未命中) | <100ms       | Feign调用监控 |
| Redis可用性     | >99.9%       | 监控告警      |

### 12.4 安全验收

- [ ] ✅ JWT使用RSA-256签名
- [ ] ✅ 密码BCrypt加密 (member-service)
- [ ] ✅ Token黑名单机制
- [ ] ✅ 验证码5分钟过期
- [ ] ✅ 验证码最多3次尝试
- [ ] ✅ 登录失败追踪 (member-service)
- [ ] ✅ 账户自动锁定 (5次失败)
- [ ] ✅ IP黑名单 (可选)

---

## 📌 总结

### ✅ 核心原则确认

1. **Auth-Service完全无数据库**
    - ❌ 无MySQL数据库
    - ✅ 只有Redis (临时数据，有TTL)

2. **所有持久化数据在Member-Service**
    - ✅ member_member (用户数据)
    - ✅ member_role (角色)
    - ✅ member_permission (权限)
    - ✅ member_device (设备)
    - ✅ member_social_connections (OAuth2绑定)

3. **Auth-Service作为编排层**
    - ✅ 纯认证授权逻辑
    - ✅ 通过Feign调用member-service
    - ✅ 防腐层隔离外部服务
    - ✅ Redis缓存提升性能

4. **职责清晰**
    - Auth-Service: 认证、Token、注册编排、权限查询
    - Member-Service: 用户数据、角色权限、设备存储、密码验证
    - Gateway: JWT本地验证、黑名单检查、限流

### ⭐ 防腐层设计价值总结

#### 1. **技术选型灵活性**

```
领域层代码完全不依赖具体技术，可以自由切换：
- 缓存：Redis → Caffeine → Composite (L1+L2)
- 安全框架：Spring Security → Apache Shiro
- 通知：阿里云短信 → 腾讯短信
- 分布式锁：Redisson → Zookeeper
- 用户域：Member → Admin → Merchant (无限扩展)
```

#### 2. **可测试性**

```java
// ✅ 使用Mock进行单元测试，无需启动真实依赖
@Test
void testLogin() {
    CachePort mockCache = Mockito.mock(CachePort.class);
    UserModulePort mockUserModule = Mockito.mock(UserModulePort.class);
    TokenManagementPort mockToken = Mockito.mock(TokenManagementPort.class);

    AuthenticationDomainService service = new AuthenticationDomainService(
            mockCache, mockUserModule, mockToken, ...
    );

    service.login(...);  // 纯业务逻辑测试
}
```

#### 3. **领域纯粹性**

```java
// ✅ 领域层代码不包含任何技术细节
@Service
public class AuthenticationDomainService {
    private final CachePort cachePort;              // ✅ 抽象接口
    private final UserModulePort userModulePort;    // ✅ 抽象接口
    private final TokenManagementPort tokenPort;    // ✅ 抽象接口

    // ❌ 不会出现以下代码
    // private final RedisTemplate redisTemplate;
    // private final MemberServiceClient memberClient;
    // private final JwtEncoder jwtEncoder;
}
```

#### 4. **演进独立性**

```
领域层与基础设施层独立演进：
- 升级Spring Boot 2.x → 3.x：只需修改基础设施层
- 从Redis 6.x → 7.x：只需修改基础设施层
- 重构Feign Client：只需修改适配器实现
- 领域层代码完全不受影响
```

#### 5. **多实现并存**

```java
// ✅ 组合缓存适配器 (L1本地 + L2 Redis)
@Component
@ConditionalOnProperty(name = "auth.cache.type", havingValue = "composite")
public class CompositeCacheAdapter implements CachePort {
    private final LocalCacheAdapter l1Cache;  // 本地缓存 (快速)
    private final RedisCacheAdapter l2Cache;  // 分布式缓存 (共享)

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        // 先查L1，未命中再查L2，回写L1
        return l1Cache.get(key, type)
                .or(() -> {
                    Optional<T> result = l2Cache.get(key, type);
                    result.ifPresent(value -> l1Cache.set(key, value, ttl));
                    return result;
                });
    }
}
```

### 🎯 架构决策总结

| 维度          | 决策                        | 理由              |
|-------------|---------------------------|-----------------|
| **架构风格**    | DDD + 六边形架构               | 领域纯粹，高内聚低耦合     |
| **防腐层**     | 端口-适配器模式                  | 技术选型灵活，易于测试     |
| **认证框架**    | Spring Security 6.x (可切换) | 成熟稳定，社区支持好      |
| **Token策略** | JWT (RSA-256)             | 无状态，Gateway本地验证 |
| **缓存策略**    | Redis (可切换)               | 分布式共享，支持L1+L2   |
| **服务调用**    | Feign (可切换)               | 声明式RPC，易于使用     |
| **时间戳**     | Unix时间戳 (Long)            | 跨语言兼容           |
| **用户域**     | 多域支持 (策略模式)               | 无限扩展新用户域        |

### 📚 关键设计模式

1. **端口-适配器模式 (Ports & Adapters)**
    - 领域层定义端口接口
    - 基础设施层实现适配器
    - 依赖倒置原则 (DIP)

2. **策略模式 (Strategy Pattern)**
    - `UserModulePortFactory` 根据 `UserDomain` 选择适配器
    - 支持无限扩展新用户域

3. **工厂模式 (Factory Pattern)**
    - `UserModulePortFactory`
    - `OAuth2ProviderFactory`

4. **组合模式 (Composite Pattern)**
    - `CompositeCacheAdapter` (L1 + L2)
    - `CompositeNotificationAdapter` (SMS + Email + Push)

5. **值对象模式 (Value Object)**
    - `Email`, `PhoneNumber`, `Password`
    - 不可变，包含验证逻辑

### 🚀 下一步行动

1. **确认架构设计** (请确认后我将传递给开发团队)
2. **与Member-Service确认内部API接口**
3. **准备DDD培训材料**
4. **启动Phase 1实施**

---

**请确认以上架构设计和技术规格（特别是第6章防腐层设计）。确认后，我将把此规格文档传递给开发团队。**

