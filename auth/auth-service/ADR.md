# Auth-Service DDD重构 - 架构决策记录 (ADR)

> **Architecture Decision Records** - 记录关键架构决策及其理由

---

## ADR-001: 采用Spring Security替代自研框架

**状态**: ✅ 已确认

**背景**:

- 现有framework-starter-security为自研安全框架
- 团队需要维护自研框架的成本较高
- Spring Security是成熟的行业标准

**决策**: 使用Spring Security 6.x作为安全框架

**理由**:

1. **成熟稳定**: Spring Security久经考验，社区活跃
2. **开箱即用**: 内置JWT、OAuth2、CSRF等功能
3. **易于集成**: 与Spring Boot完美集成
4. **降低维护成本**: 无需维护自研框架
5. **人才储备**: 更容易找到熟悉Spring Security的开发者

**后果**:

- ✅ 减少自研框架维护成本
- ✅ 提升安全性和稳定性
- ⚠️ 需要学习Spring Security配置
- ⚠️ 现有framework-starter-security逐步废弃

---

## ADR-002: 无Session设计，纯JWT Token认证

**状态**: ✅ 已确认

**背景**:

- 传统Session需要服务端存储状态
- 微服务架构需要水平扩展
- 用户希望使用JWT但不确定是否需要Session

**决策**: 采用无状态JWT Token，不使用Session

**理由**:

1. **水平扩展**: 无Session，支持分布式部署
2. **性能优势**: 减少服务端存储压力
3. **简化架构**: 无需Session共享机制
4. **移动端友好**: 移动端不依赖Cookie

**实现方式**:

```
认证流程:
1. 用户登录 → 签发AccessToken(1小时) + RefreshToken(30天)
2. 客户端携带AccessToken访问资源
3. Gateway本地验证JWT (无需调用auth-service)
4. AccessToken过期 → 使用RefreshToken刷新

登出机制:
1. 客户端调用/auth/logout
2. 将Token的jti加入Redis黑名单
3. Redis Pub/Sub通知Gateway
4. Gateway本地缓存黑名单(10s TTL)
```

**后果**:

- ✅ 支持分布式部署
- ✅ 性能优异
- ✅ 架构简单
- ⚠️ 需要维护Token黑名单
- ⚠️ Token泄露风险 (通过短TTL + 黑名单缓解)

---

## ADR-003: 时间字段统一使用Unix时间戳

**状态**: ✅ 已确认

**背景**:

- 传统使用`LocalDateTime`、`Instant`等Java时间类型
- 前后端时区处理复杂
- 用户要求前端做本地化

**决策**: 所有时间字段使用Unix时间戳(Long类型，秒级)

**理由**:

1. **跨平台一致性**: 时间戳是语言无关的
2. **简化时区处理**: 后端不关心时区，由前端本地化
3. **序列化简单**: JSON中直接是数字
4. **数据库兼容性**: 支持各种数据库类型

**实现规范**:

```java
// ✅ 正确
public class MemberPrincipal {
    private Long createdAt;  // Unix时间戳(秒)
    private Long updatedAt;
}

// ❌ 错误
public class MemberPrincipal {
    private Instant createdAt;
    private LocalDateTime updatedAt;
}

// 值对象封装
public record UnixTimestamp(Long value) {
    public static UnixTimestamp now() {
        return new UnixTimestamp(System.currentTimeMillis() / 1000);
    }
}
```

**数据库映射**:

```sql
CREATE TABLE member_principal (
    id BIGINT PRIMARY KEY,
    created_at BIGINT NOT NULL COMMENT '创建时间戳(秒)',
    updated_at BIGINT NOT NULL COMMENT '更新时间戳(秒)'
);
```

**前端本地化示例**:

```javascript
// 前端接收到时间戳
const createdAt = 1699516800;  // 2023-11-09 10:00:00 UTC

// 本地化显示
const date = new Date(createdAt * 1000);
const localTime = date.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });
// 输出: "2023/11/9 18:00:00"
```

**后果**:

- ✅ 时区处理简单
- ✅ 前后端解耦
- ✅ 数据库存储高效
- ⚠️ 可读性稍差 (需要工具转换)
- ⚠️ 秒级精度 (毫秒场景需特殊处理)

---

## ADR-004: 多用户域架构 (Member/Admin分离)

**状态**: ✅ 已确认

**背景**:

- 当前有member域 (C端用户)
- 未来会有admin模块 (B端后台用户)
- 两类用户权限模型不同

**决策**: 引入抽象`UserPrincipal`接口，支持多用户域

**架构设计**:

```java
// 共享内核 - 抽象用户接口
public interface UserPrincipal {
    String getUserId();

    UserDomain getUserDomain();  // MEMBER, ADMIN

    String getUsername();

    Set<String> getAuthorities();

    Map<String, Object> toClaims();
}

// 会员域实现
public class MemberPrincipal implements UserPrincipal {
    private MemberId memberId;

    @Override
    public UserDomain getUserDomain() {
        return UserDomain.MEMBER;
    }
}

// 后台用户域实现 (预留)
public class AdminPrincipal implements UserPrincipal, OrganizationalEntity {
    private AdminId adminId;
    private Set<Long> departmentIds;  // 所属部门

    @Override
    public UserDomain getUserDomain() {
        return UserDomain.ADMIN;
    }
}
```

**JWT Token中的体现**:

```json
{
  "sub": "user-id-123",
  "userDomain": "MEMBER",
  "username": "john_doe",
  "authorities": [
    "user:read",
    "user:write"
  ]
}
```

**数据库隔离**:

```
member_principal表 → 会员数据
admin_principal表  → 后台用户数据 (未来)

共享表:
role表
permission表
user_role_assignment表 (带user_domain字段区分)
```

**理由**:

1. **领域隔离**: Member和Admin是不同的业务领域
2. **扩展性**: 未来可轻松添加新用户域 (如PartnerPrincipal)
3. **权限差异化**: 不同用户域可有不同的权限模型
4. **数据安全**: 物理隔离，防止数据泄露

**后果**:

- ✅ 高扩展性
- ✅ 领域清晰
- ⚠️ 需要工厂模式统一创建
- ⚠️ 仓储层需要根据domain查询

---

## ADR-005: Gateway承担认证鉴权限流职责

**状态**: ✅ 已确认

**背景**:

- 传统架构每个服务都需要集成认证逻辑
- 用户询问Gateway是否应该承担认证鉴权

**决策**: Gateway作为统一入口，承担认证鉴权限流职责

**架构图**:

```
Client → Gateway (JWT验证 + 权限检查 + 限流) → Backend Services
            ↓
         Redis (黑名单 + 权限缓存)
            ↓
      Auth-Service (仅登录/权限管理调用)
```

**Gateway职责**:

1. **JWT Token本地验证** - 使用公钥验证签名，检查过期时间
2. **黑名单检查** - 查询Redis本地缓存 (Caffeine + Redis)
3. **权限预检查** - 简单的URL权限匹配
4. **限流控制** - 基于用户/IP的令牌桶算法
5. **转发请求** - 添加用户上下文Header

**Auth-Service职责**:

1. **用户认证** - 登录、OAuth2、微信登录
2. **Token签发** - 生成AccessToken + RefreshToken
3. **权限管理** - 角色、权限CRUD
4. **黑名单管理** - 登出时加入黑名单

**通信方式**:

```
Gateway ← Redis Pub/Sub ← Auth-Service
(订阅黑名单变更)        (发布黑名单事件)

Gateway ← Redis缓存 ← Auth-Service
(读取权限缓存)      (权限变更时刷新)
```

**理由**:

1. **性能优化**: Gateway本地验证JWT，无需调用服务 (<5ms)
2. **减少网络调用**: 避免每次请求都调用auth-service
3. **架构解耦**: 业务服务不关心认证逻辑
4. **统一安全策略**: 所有流量经过Gateway，安全策略一致

**后果**:

- ✅ 性能极佳
- ✅ 业务服务简化
- ⚠️ Gateway复杂度增加
- ⚠️ 需要维护Redis缓存一致性

---

## ADR-006: 组织架构功能预留，当前不实现

**状态**: ✅ 已确认

**背景**:

- Function.md中包含部门、职位管理功能
- 用户明确当前不需要组织架构
- 但未来Admin模块可能需要

**决策**: 当前不实现部门、职位功能，仅预留扩展接口

**预留设计**:

```java
// 扩展接口 (Shared Kernel)
public interface OrganizationalEntity {
    Set<Long> getOrganizationIds();

    Long getPrimaryOrganizationId();
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
    Set<Long> calculateDataScope(UserPrincipal principal);
}

// 默认实现 (当前返回null，表示无组织限制)
public class DefaultDataScopeProvider implements DataScopeProvider {
    @Override
    public Set<Long> calculateDataScope(UserPrincipal principal) {
        if (principal instanceof OrganizationalEntity org) {
            return org.getOrganizationIds();
        }
        return null; // 无限制
    }
}
```

**未来实现路径**:

1. 创建`organization`子包
2. 实现`Department`、`Position`聚合
3. `AdminPrincipal`实现`OrganizationalEntity`接口
4. 实现具体的`DataScopeProvider`
5. **不影响现有`MemberPrincipal`逻辑**

**理由**:

1. **YAGNI原则**: 当前不需要，不提前实现
2. **降低复杂度**: 减少初期开发工作量
3. **保持扩展性**: 接口预留，未来无需重构
4. **专注核心**: 优先实现认证授权核心功能

**后果**:

- ✅ 降低初期工作量
- ✅ 未来扩展无需重构
- ✅ 架构保持清晰
- ⚠️ 需要良好的接口设计

---

## ADR-007: 使用RSA非对称加密保护JWT

**状态**: ✅ 推荐

**背景**:

- JWT可使用HMAC对称加密或RSA非对称加密
- Gateway需要验证JWT但不应该签发Token

**决策**: 使用RSA-256非对称加密

**密钥分配**:

```
Auth-Service:
  - 私钥 (jwt_private_key.pem) → 签发Token
  - 公钥 (jwt_public_key.pem) → 验证Token

Gateway:
  - 公钥 (jwt_public_key.pem) → 仅验证Token
```

**生成密钥对**:

```bash
# 生成私钥
openssl genrsa -out jwt_private_key.pem 2048

# 导出公钥
openssl rsa -in jwt_private_key.pem -pubout -out jwt_public_key.pem
```

**配置示例**:

```yaml
# Auth-Service
pot:
  jwt:
    private-key: classpath:keys/jwt_private_key.pem
    public-key: classpath:keys/jwt_public_key.pem

# Gateway
pot:
  jwt:
    public-key: classpath:keys/jwt_public_key.pem
```

**理由**:

1. **安全性**: Gateway即使被入侵也无法签发Token
2. **职责分离**: 签发和验证物理隔离
3. **密钥管理**: 私钥仅存储在Auth-Service
4. **行业最佳实践**: OAuth2标准推荐

**后果**:

- ✅ 安全性极高
- ✅ Gateway无法签发Token
- ⚠️ 性能稍低于HMAC (可忽略)
- ⚠️ 密钥管理复杂度增加

---

## ADR-008: 使用DDD四层架构 + 六边形架构

**状态**: ✅ 已确认

**背景**:

- 传统三层架构导致业务逻辑分散
- 希望通过DDD提升代码质量

**决策**: 采用DDD分层架构 + 端口-适配器模式

**分层结构**:

```
┌─────────────────────────────────────────┐
│  Interfaces Layer (接口层)               │
│  - REST API                             │
│  - Event Listener                       │
│  - Scheduled Tasks                      │
├─────────────────────────────────────────┤
│  Application Layer (应用层)              │
│  - Application Services (用例编排)      │
│  - DTO / Assembler                      │
│  - Command / Query                      │
├─────────────────────────────────────────┤
│  Domain Layer (领域层) ⭐核心             │
│  - Aggregates (聚合根)                  │
│  - Entities (实体)                      │
│  - Value Objects (值对象)               │
│  - Domain Services (领域服务)           │
│  - Repository Interfaces (仓储接口)     │
│  - Domain Events (领域事件)             │
├─────────────────────────────────────────┤
│  Infrastructure Layer (基础设施层)       │
│  - Repository Impl (仓储实现)           │
│  - Cache (Redis)                        │
│  - External Services (Adapters)         │
│  - Messaging (Pub/Sub)                  │
└─────────────────────────────────────────┘
```

**依赖规则**:

```
✅ 允许:
interfaces → application → domain
infrastructure → domain (仅实现接口)

❌ 禁止:
domain → infrastructure
domain → application
domain → interfaces
```

**架构测试**:

```java

@ArchTest
public static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");
```

**理由**:

1. **业务逻辑内聚**: 领域层零依赖，纯业务逻辑
2. **易于测试**: 领域层可独立测试
3. **技术无关**: 领域层不依赖框架
4. **可维护性**: 清晰的分层边界

**后果**:

- ✅ 代码质量显著提升
- ✅ 业务逻辑清晰
- ✅ 易于测试
- ⚠️ 学习曲线
- ⚠️ 代码量增加 (但更清晰)

---

## ADR-009: RefreshToken滑动窗口自动续期

**状态**: ✅ 已确认

**背景**:

- 传统RefreshToken有固定过期时间(如30天)
- 用户需要定期重新登录，体验不佳
- 希望活跃用户可以持续保持登录状态

**决策**: 采用滑动窗口机制自动续期RefreshToken

**实现方式**:

```java
public class JwtToken {
    private Long lastRefreshedAt;  // 最后刷新时间戳
    private static final long REFRESH_WINDOW = 7 * 24 * 3600; // 7天续期窗口

    public TokenPair refresh(Long currentTimestamp) {
        // 生成新的AccessToken
        JwtToken newAccessToken = createAccessToken(...);

        // 滑动窗口续期：如果在7天内刷新，RefreshToken也续期
        if (shouldRenewRefreshToken(currentTimestamp)) {
            JwtToken newRefreshToken = createRefreshToken(...);
            return new TokenPair(newAccessToken, newRefreshToken);
        }

        return new TokenPair(newAccessToken, this); // 复用旧RefreshToken
    }

    private boolean shouldRenewRefreshToken(Long currentTimestamp) {
        long timeSinceLastRefresh = currentTimestamp - this.lastRefreshedAt;
        return timeSinceLastRefresh < REFRESH_WINDOW;
    }
}
```

**续期策略**:

```
RefreshToken初始有效期: 30天
滑动窗口: 7天

场景1: 用户3天后刷新AccessToken
- 距离上次刷新 < 7天
- ✅ RefreshToken续期30天
- 新过期时间 = 当前时间 + 30天

场景2: 用户10天后刷新AccessToken
- 距离上次刷新 > 7天
- ❌ RefreshToken不续期
- 继续使用旧RefreshToken，剩余20天有效期

场景3: 用户每天都使用
- 每次刷新都 < 7天
- ✅ 持续续期
- 实现"长期免登录"
```

**理由**:

1. **用户体验优化**: 活跃用户无需频繁登录
2. **安全平衡**: 长期不活跃用户仍会过期
3. **灵活控制**: 通过REFRESH_WINDOW参数调整策略

**后果**:

- ✅ 活跃用户体验提升
- ✅ 长期不活跃账户自动过期
- ⚠️ 需要记录`lastRefreshedAt`字段
- ⚠️ RefreshToken会频繁更换 (需客户端处理)

---

## ADR-010: 多种登录方式支持

**状态**: ✅ 已确认

**背景**:

- 用户需要多种便捷的登录方式
- 不同场景适合不同的认证方式
- 安全性和便利性需要平衡

**决策**: 支持4种登录方式

**登录方式清单**:

### 1️⃣ 密码登录

```java
POST /auth/login/

password {
    "identifier":"john_doe",      // 用户名/邮箱/手机号
            "password":"password123",
            "userDomain":"MEMBER",
            "deviceInfo":{ ...}
}
```

- 适用场景: Web端、App端
- 标识符: 支持用户名/邮箱/手机号任意一种

### 2️⃣ 验证码登录 (无密码)

```java
POST /auth/login/

code {
    "recipient":"user@example.com",  // 邮箱或手机号
            "code":"123456",
            "userDomain":"MEMBER",
            "deviceInfo":{ ...}
}
```

- 适用场景: 快速登录、忘记密码
- 验证码有效期: 5分钟
- 最多尝试次数: 3次
- 发送频率限制: 60秒/次

### 3️⃣ OAuth2三方登录

```java
GET /oauth2/authorize/github?redirect_uri=...
GET /oauth2/callback/github?code=xxx&state=yyy
```

- 支持提供商: GitHub, Google
- 预留扩展点: `OAuth2ProviderFactory`接口
- 未来可扩展: GitLab, 企业微信, 钉钉等

### 4️⃣ 微信扫码登录

```java
GET /auth/wechat/qrcode      // 获取二维码
GET /auth/wechat/callback    // 扫码回调
```

- 适用场景: Web端扫码登录
- 集成: weixin-java-open

**实现架构**:

```
LoginMethod枚举
├─ PASSWORD
├─ VERIFICATION_CODE
├─ OAUTH2
└─ WECHAT_SCAN

AuthenticationDomainService接口
├─ authenticateWithPassword(...)
├─ authenticateWithVerificationCode(...)
├─ authenticateWithOAuth2(...)
└─ authenticateWithWechatScan(...)
```

**理由**:

1. **覆盖主流场景**: 满足不同用户群体需求
2. **安全性**: 验证码登录提供无密码安全方式
3. **便利性**: OAuth2和微信扫码降低注册门槛
4. **扩展性**: 预留接口支持未来添加新方式

**后果**:

- ✅ 用户体验提升
- ✅ 注册转化率提高
- ⚠️ 开发工作量增加
- ⚠️ 需要集成短信/邮件服务

---

## ADR-011: 设备管理与异地登录检测

**状态**: ✅ 已确认

**背景**:

- 用户可能在多个设备登录
- 需要安全管控和设备追踪
- 异地登录可能存在安全风险

**决策**: 实现设备管理系统，记录用户登录设备

**核心聚合**: `UserDevice`

```java
public class UserDevice {
    private DeviceId deviceId;
    private UserPrincipal principal;
    private DeviceInfo deviceInfo;       // 设备信息
    private IpAddress lastIpAddress;     // 最后登录IP
    private Long firstLoginAt;           // 首次登录
    private Long lastLoginAt;            // 最后登录
    private DeviceStatus status;         // ACTIVE/KICKED/EXPIRED
    private String currentRefreshToken;  // 当前Token

    public void kick(Long timestamp) {
        this.status = DeviceStatus.KICKED;
        // 发布事件 → 撤销该设备的所有Token
    }
}
```

**设备信息结构**:

```java
public record DeviceInfo(
        String deviceId,        // 设备唯一标识 (客户端生成UUID)
        DeviceType deviceType,  // WEB, IOS, ANDROID, WECHAT_MP
        String deviceName,      // "Chrome on macOS"
        String osVersion,       // "macOS 14.0"
        String appVersion       // "1.0.0"
) {
}
```

**功能列表**:

1. **设备登录记录**

```java
每次登录时:
        1.记录设备信息
2.记录IP地址
3.关联RefreshToken
4.更新最后登录时间
```

2. **查看在线设备**

```java
GET /auth/devices
Response:
        [
        {
        "deviceId":"xxx",
        "deviceType":"WEB",
        "deviceName":"Chrome on macOS",
        "lastIpAddress":"123.456.789.0",
        "lastLoginAt":1699516800,
        "status":"ACTIVE"
        },
        ...
        ]
```

3. **踢出设备**

```java
POST /auth/devices/{deviceId}/kick

流程:
        1.设备状态 →KICKED
2.撤销该设备的RefreshToken
3.发布DeviceKickedEvent
4.Gateway收到事件，该设备Token加入黑名单
```

4. **异地登录检测**

```java
public boolean isSuspicious(IpAddress currentIp) {
    // IP地域比对
    return !this.lastIpAddress.isSameRegion(currentIp);
}

// 异地登录时
if(isSuspiciousLogin){
        // 发送通知 (framework-starter-touch)
        notificationService.

send(user, "检测到异地登录: "+location);
}
```

**数据库设计**:

```sql
CREATE TABLE user_device (
    id BIGINT PRIMARY KEY,
    device_id VARCHAR(64) UNIQUE NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_domain VARCHAR(20) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    device_name VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    last_ip_address VARCHAR(45),
    first_login_at BIGINT NOT NULL,
    last_login_at BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_refresh_token VARCHAR(500),
    INDEX idx_user_device (user_id, user_domain),
    INDEX idx_device_id (device_id)
);
```

**理由**:

1. **安全管控**: 用户可查看和管理登录设备
2. **异常检测**: 及时发现账户被盗用
3. **审计追溯**: 记录完整的登录历史
4. **用户体验**: 移动端和Web端设备隔离管理

**后果**:

- ✅ 安全性提升
- ✅ 用户可控性增强
- ✅ 异常行为可追溯
- ⚠️ 存储成本增加
- ⚠️ 需要客户端生成deviceId

---

## ADR-012: 验证码登录与无密码认证

**状态**: ✅ 已确认

**背景**:

- 用户经常忘记密码
- 验证码登录更便捷
- 降低注册门槛

**决策**: 实现邮箱/手机号验证码登录

**核心聚合**: `VerificationCode`

```java
public class VerificationCode {
    private CodeId codeId;
    private CodeType codeType;        // EMAIL, SMS
    private String recipient;         // 接收者
    private String code;              // 6位数字
    private CodePurpose purpose;      // LOGIN, REGISTER, RESET_PASSWORD
    private Long createdAt;
    private Long expiresAt;
    private int verifyAttempts;       // 验证尝试次数
    private boolean verified;

    // 验证逻辑
    public void verify(String inputCode, Long currentTimestamp) {
        if (isExpired(currentTimestamp)) {
            throw new CodeExpiredException();
        }
        if (verifyAttempts >= 3) {
            throw new TooManyAttemptsException();
        }
        if (!this.code.equals(inputCode)) {
            verifyAttempts++;
            throw new InvalidCodeException();
        }
        this.verified = true;
    }
}
```

**验证码策略**:

```
有效期: 5分钟
长度: 6位数字
最多尝试: 3次
发送频率: 60秒/次 (防刷)
用途:
  - LOGIN: 登录
  - REGISTER: 注册
  - RESET_PASSWORD: 重置密码
  - BIND_EMAIL: 绑定邮箱
  - BIND_PHONE: 绑定手机号
```

**发送验证码流程**:

```java
POST /auth/code/

send {
    "type":"EMAIL",               // EMAIL or SMS
            "recipient":"user@example.com",
            "purpose":"LOGIN"
}

流程:
        1.

检查发送频率(Redis限流)
2.生成6位随机数字
3.

存储到Redis(5分钟TTL)
4.调用framework-starter-touch发送
   -邮件:使用邮件模板
   -短信:调用三方短信服务
```

**验证码登录流程**:

```java
POST /auth/login/

code {
    "recipient":"user@example.com",
            "code":"123456",
            "userDomain":"MEMBER"
}

流程:
        1.

查询验证码(Redis)
2.验证有效期、尝试次数
3.验证码匹配
4.

查询用户(如不存在，自动注册)
5.签发Token
6.删除验证码
```

**与framework-starter-touch集成**:

```java

@Service
public class VerificationCodeNotifier {
    private final TouchService touchService;

    public void sendEmailCode(String email, String code) {
        touchService.sendEmail(EmailTemplate.builder()
                .to(email)
                .subject("登录验证码")
                .template("login_code")
                .param("code", code)
                .param("expireMinutes", 5)
                .build());
    }

    public void sendSmsCode(String phone, String code) {
        touchService.sendSms(SmsTemplate.builder()
                .phone(phone)
                .template("LOGIN_CODE")
                .param("code", code)
                .build());
    }
}
```

**理由**:

1. **用户体验**: 无需记忆密码
2. **降低门槛**: 一键注册登录
3. **安全性**: 验证码时效短，难以被盗用
4. **通用性**: 支持登录、注册、找回密码等多场景

**后果**:

- ✅ 注册转化率提升
- ✅ 用户体验改善
- ⚠️ 短信成本 (建议邮箱优先)
- ⚠️ 需要防刷机制

---

## ADR-013: 多种注册方式与验证流程

**状态**: ✅ 已确认

**背景**:

- 用户注册是认证系统的入口
- 不同场景需要不同的注册方式
- 需要验证邮箱/手机号防止滥用

**决策**: 支持4种注册方式 + 邮箱/手机号验证机制

**注册方式清单**:

### 1️⃣ 密码注册 (传统方式)

```java
POST /auth/register/

password {
    "username":"john_doe",
            "email":"user@example.com",
            "phone":"+8613800138000",
            "password":"SecurePass123!",
            "userDomain":"MEMBER"
}

流程:
        1.验证用户名/邮箱/手机号唯一性
2.

验证密码策略(最小8位、大小写数字特殊字符)
3.创建UserRegistration聚合
4.

创建MemberPrincipal(状态:PENDING_VERIFICATION)
5.发送验证邮件/短信
6.

返回注册成功(提示验证邮箱)

Response:
        {
        "registrationId":"reg-xxx",
        "userId":"user-123",
        "status":"PENDING_VERIFICATION",
        "message":"注册成功，请验证邮箱"
        }
```

### 2️⃣ 验证码注册 (快捷方式)

```java
// 步骤1: 发送验证码
POST /auth/code/

send {
    "type":"EMAIL",
            "recipient":"user@example.com",
            "purpose":"REGISTER"
}

// 步骤2: 验证码注册
POST /auth/register/

code {
    "recipient":"user@example.com",
            "code":"123456",
            "userDomain":"MEMBER"
}

流程:
        1.验证验证码
2.检查邮箱/手机号是否已注册
3.

自动生成用户名(如:user_1234567890)
4.

创建MemberPrincipal(状态:ACTIVE，因为已验证)
5.**自动登录，返回Token**

Response:
        {
        "userId":"user-123",
        "username":"user_1234567890",
        "accessToken":"...",
        "refreshToken":"..."
        }
```

### 3️⃣ OAuth2注册 (一键注册)

```java
GET /oauth2/authorize/github?redirect_uri=...
GET /oauth2/callback/github?code=xxx

流程:
        1.用户授权GitHub/Google账号
2.

获取OAuth2用户信息(email, avatar, name)
3.检查email是否已注册
   -已注册:直接登录
   -未注册:自动创建账户
4.

生成用户名(github_username 或 email前缀)
5.

创建MemberPrincipal(状态:ACTIVE)
6.**自动登录，返回Token**
```

### 4️⃣ 微信扫码注册

```java
GET /auth/wechat/qrcode
GET /auth/wechat/callback?code=xxx

流程:
        1.获取微信二维码
2.用户扫码授权
3.

获取微信用户信息(openid, unionid, nickname, avatar)
4.检查是否已注册
   -已注册:直接登录
   -未注册:自动创建账户
5.

创建MemberPrincipal(状态:ACTIVE)
6.**自动登录，返回Token**
```

---

### 验证流程设计

**邮箱验证**:

```java
注册后发送验证邮件:
        -验证链接:https://app.com/verify-email?token=xxx
        -有效期:24小时
-点击链接后 →POST /auth/register/verify-email

流程:
        1.解析token
2.查询UserRegistration
3.检查是否过期
4.更新MemberPrincipal状态 →ACTIVE
5.发布EmailVerifiedEvent
6.可选:自动登录返回Token
```

**手机号验证**:

```java
注册后发送短信验证码:
        -验证码:6位数字
-有效期:5分钟
-输入验证码 →POST /auth/register/verify-phone

流程:
        1.验证验证码
2.更新MemberPrincipal状态 →ACTIVE
3.发布PhoneVerifiedEvent
4.可选:自动登录返回Token
```

---

### 聚合设计

**UserRegistration聚合**:

```java
public class UserRegistration {
    private RegistrationId registrationId;
    private UserDomain userDomain;
    private RegistrationMethod method;
    private RegistrationData registrationData;
    private RegistrationStatus status;
    private Long createdAt;
    private Long expiresAt;  // 验证链接24小时过期

    public MemberPrincipal complete(String verificationCode) {
        // 创建MemberPrincipal
        // 发布RegistrationCompletedEvent
    }

    public static UserRegistration createWithPassword(...) {
    }

    public static UserRegistration createWithVerificationCode(...) {
    }

    public static UserRegistration createWithOAuth2(...) {
    }
}

public enum RegistrationMethod {
    PASSWORD,              // 密码注册
    VERIFICATION_CODE,     // 验证码注册
    OAUTH2,                // OAuth2注册
    WECHAT_SCAN            // 微信扫码注册
}

public enum RegistrationStatus {
    PENDING_VERIFICATION,  // 待验证 (密码注册)
    VERIFIED,              // 已验证
    COMPLETED,             // 已完成
    EXPIRED                // 已过期 (24小时)
}
```

---

### 唯一性检查策略

**检查时机**:

```java
注册时检查:
        1.用户名:全局唯一
2.邮箱:全局唯一
3.手机号:全局唯一

实现:

public interface MemberPrincipalRepository {
    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    boolean existsByPhone(PhoneNumber phone);
}

注册服务:

public class UserRegistrationService {
    public void validateUniqueness(Username username, Email email, PhoneNumber phone) {
        if (memberRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (email != null && memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (phone != null && memberRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyExistsException();
        }
    }
}
```

---

### 账户状态流转

```
密码注册:
PENDING_VERIFICATION → (验证邮箱/手机号) → ACTIVE

验证码注册:
直接 ACTIVE (因为验证码已验证)

OAuth2/微信注册:
直接 ACTIVE (因为三方账号已验证)
```

---

### 数据库表设计

```sql
-- 用户注册表
CREATE TABLE user_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id VARCHAR(64) UNIQUE NOT NULL,
    user_domain VARCHAR(20) NOT NULL,
    registration_method VARCHAR(30) NOT NULL,
    username VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(20),
    password_hash VARCHAR(255),
    verification_token VARCHAR(255) UNIQUE,
    status VARCHAR(30) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    INDEX idx_email (email),
    INDEX idx_verification_token (verification_token),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户注册表';

-- 会员表新增字段
ALTER TABLE member_principal
ADD COLUMN registration_method VARCHAR(30) COMMENT '注册方式';
```

---

**理由**:

1. **降低门槛**: 验证码/OAuth2注册无需记忆密码
2. **提升转化率**: 自动登录减少流失
3. **安全性**: 密码注册需验证邮箱/手机号
4. **灵活性**: 支持多种注册方式适应不同场景

**后果**:

- ✅ 注册转化率提升
- ✅ 用户体验优化
- ✅ 防止垃圾注册
- ⚠️ 需要邮件/短信服务成本
- ⚠️ OAuth2注册需要绑定邮箱/手机号 (可选)

---

## 决策总览 (更新)

| ADR         | 决策                  | 状态    | 影响 |
|-------------|---------------------|-------|----|
| ADR-001     | Spring Security替代自研 | ✅ 已确认 | 高  |
| ADR-002     | 无Session，纯JWT       | ✅ 已确认 | 高  |
| ADR-003     | 时间戳标准化              | ✅ 已确认 | 中  |
| ADR-004     | 多用户域架构              | ✅ 已确认 | 高  |
| ADR-005     | Gateway认证鉴权         | ✅ 已确认 | 高  |
| ADR-006     | 组织架构预留              | ✅ 已确认 | 低  |
| ADR-007     | RSA非对称加密            | ✅ 推荐  | 中  |
| ADR-008     | DDD四层架构             | ✅ 已确认 | 高  |
| ADR-009     | RefreshToken自动续期    | ✅ 已确认 | 中  |
| ADR-010     | 多种登录方式              | ✅ 已确认 | 高  |
| ADR-011     | 设备管理                | ✅ 已确认 | 中  |
| ADR-012     | 验证码登录               | ✅ 已确认 | 中  |
| **ADR-013** | **多种注册方式与验证**       | ✅ 已确认 | 高  |

---

**最后更新**: 2025-11-09  
**版本**: v3.0

