# 认证控制器重构实施指南

## 📋 文档信息

- **项目**: Zing认证服务重构
- **架构方案**: 方案一 - 资源导向型
- **版本**: v2.0
- **状态**: 实施中
- **日期**: 2025-10-25

---

## 🎯 重构目标

将现有的5个认证Controller（AuthController、LoginController、RegisterController、OAuth2Controller、SignInOrRegisterController）重构为符合RESTful规范和OAuth
2.0标准的工业级架构。

---

## 📐 新架构概览

### Controller结构

```
auth-service/
└── controller/
    └── v1/                                     # API版本控制
        ├── AuthenticationController.java       # 认证会话管理（核心）
        ├── CredentialController.java           # 凭证管理
        ├── OAuthProviderController.java        # OAuth提供商管理
        ├── TokenController.java                # Token管理
        ├── RegistrationController.java         # 用户注册
        └── AccountBindingController.java       # 账户绑定
```

### 核心设计理念

1. **统一入口** - 所有登录方式统一为 `POST /api/v1/auth/sessions`
2. **grantType区分** - 通过grantType字段区分认证方式（符合OAuth 2.0）
3. **多态处理** - 使用Jackson多态反序列化，自动路由到对应的处理逻辑
4. **策略模式** - 每种认证方式是一个Strategy
5. **RESTful** - 完全符合REST规范，资源为中心

---

## 📦 已创建的文件

### 1. 核心模型层

```
dto/session/
├── CreateSessionRequest.java          # 统一登录请求基类
├── AuthSession.java                    # 认证会话响应
├── AuthenticationService.java          # 认证服务接口
└── grant/                              # 各种Grant类型
    ├── PasswordGrantRequest.java       # 密码登录
    ├── SmsCodeGrantRequest.java        # 短信验证码登录
    ├── EmailCodeGrantRequest.java      # 邮箱验证码登录
    ├── AuthorizationCodeGrantRequest.java  # OAuth2登录
    ├── WeChatQrCodeGrantRequest.java   # 微信扫码登录
    └── RefreshTokenGrantRequest.java   # 刷新Token
```

### 2. Controller层

```
controller/v1/
├── AuthenticationController.java      # ✅ 已创建
├── CredentialController.java          # ✅ 已创建
├── OAuthProviderController.java       # ✅ 已创建
├── TokenController.java                # 待创建
├── RegistrationController.java         # 待创建
└── AccountBindingController.java       # 待创建
```

---

## 🚀 完整实施步骤

### 阶段1: 创建基础结构 ✅ (已完成)

- [x] 创建新的包结构 `controller/v1`
- [x] 创建 `CreateSessionRequest` 基类
- [x] 创建所有Grant类型的Request
- [x] 创建 `AuthSession` 响应模型
- [x] 创建 `AuthenticationService` 接口

### 阶段2: 实现核心Controller ✅ (已完成)

- [x] `AuthenticationController` - 认证会话管理
- [x] `CredentialController` - 凭证管理
- [x] `OAuthProviderController` - OAuth提供商管理

### 阶段3: 实现剩余Controller (进行中)

#### 3.1 TokenController

```java

@RestController
@RequestMapping("/api/v1/auth/tokens")
public class TokenController {

    // POST /api/v1/auth/tokens/refresh      刷新Token
    // POST /api/v1/auth/tokens/revoke       撤销Token
    // POST /api/v1/auth/tokens/validate     验证Token
}
```

#### 3.2 RegistrationController

```java

@RestController
@RequestMapping("/api/v1/auth/registrations")
public class RegistrationController {

    // POST /api/v1/auth/registrations                   注册
    // GET  /api/v1/auth/registrations/availability      检查可用性
}
```

#### 3.3 AccountBindingController

```java

@RestController
@RequestMapping("/api/v1/auth/bindings")
public class AccountBindingController {

    // POST   /api/v1/auth/bindings              绑定OAuth账号
    // DELETE /api/v1/auth/bindings/{provider}   解绑OAuth账号
    // GET    /api/v1/auth/bindings              获取绑定列表
}
```

### 阶段4: 实现Service层

#### 4.1 AuthenticationServiceImpl

```java

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final MemberFacade memberFacade;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;

    @Override
    public AuthSession authenticate(CreateSessionRequest request) {
        // 核心实现：
        // 1. request自动调用对应的authenticate方法（多态）
        // 2. 验证凭证
        // 3. 获取或创建用户
        // 4. 生成Token
        // 5. 创建会话
        // 6. 返回AuthSession

        return request.authenticate(this);
    }

    // 每种Grant类型的具体实现
    public AuthSession authenticateByPassword(PasswordGrantRequest request) {
        // 1. 识别username类型（用户名/邮箱/手机号）
        // 2. 查询用户
        // 3. 验证密码
        // 4. 生成Token
        // 5. 创建会话
    }

    public AuthSession authenticateBySmsCode(SmsCodeGrantRequest request) {
        // 1. 验证手机号和验证码
        // 2. 查询用户（不存在则自动注册）
        // 3. 生成Token
        // 4. 创建会话
    }

    // ... 其他认证方式的实现
}
```

#### 4.2 会话管理设计

```java
/**
 * 会话存储在Redis中
 *
 * Key设计：
 * - session:{sessionId}              会话详情
 * - user:sessions:{userId}           用户的所有会话ID（Set）
 * - token:access:{accessToken}       AccessToken映射到SessionId
 * - token:refresh:{refreshToken}     RefreshToken映射到SessionId
 * - token:blacklist:{token}          Token黑名单
 *
 * 过期策略：
 * - AccessToken: 1小时
 * - RefreshToken: 7天
 * - Session: 与RefreshToken同步
 */
```

### 阶段5: 兼容层实现 (可选)

为了保证向后兼容，保留旧的Controller作为适配器：

```java

@RestController
@RequestMapping("/auth")
@Deprecated
public class LegacyAuthController {

    private final AuthenticationController newController;

    @PostMapping("/login")
    @Deprecated
    public R<AuthResponse> login(@RequestBody OldLoginRequest oldRequest) {
        // 1. 转换为新的Request格式
        PasswordGrantRequest newRequest = convertToPasswordGrant(oldRequest);

        // 2. 调用新Controller
        R<AuthSession> response = newController.createSession(newRequest);

        // 3. ��换为旧的Response格式
        return convertToOldResponse(response);
    }

    // 其他旧接口的适配...
}
```

### 阶段6: 测试

#### 6.1 单元测试

```java

@SpringBootTest
class AuthenticationControllerTest {

    @Test
    void testPasswordLogin() {
        PasswordGrantRequest request = new PasswordGrantRequest();
        request.setGrantType("password");
        request.setUsername("test@example.com");
        request.setPassword("Password123!");

        R<AuthSession> response = controller.createSession(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getAccessToken());
    }

    @Test
    void testSmsCodeLogin() {
        // ...
    }

    @Test
    void testOAuth2Login() {
        // ...
    }
}
```

#### 6.2 集成测试

```java

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCompleteLoginFlow() {
        // 1. 发送验证码
        // 2. 使用验证码登录
        // 3. 使用accessToken访问受保护资源
        // 4. 刷新Token
        // 5. 登出
    }
}
```

---

## 📝 API使用示例

### 1. 密码登录

```bash
curl -X POST http://localhost:10000/api/v1/auth/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "user@example.com",
    "password": "Password123!",
    "clientId": "web",
    "rememberMe": true
  }'
```

**响应**:

```json
{
  "code": 200,
  "success": true,
  "message": "登录成功",
  "data": {
    "sessionId": "session_abc123",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_xyz789",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshExpiresIn": 604800,
    "userInfo": {
      "userId": 123456,
      "nickname": "John Doe",
      "email": "user@example.com",
      "avatarUrl": "https://example.com/avatar.jpg",
      "roles": [
        "USER"
      ],
      "permissions": [
        "user:read",
        "post:create"
      ]
    },
    "isNewUser": false,
    "authMethod": "password"
  }
}
```

### 2. 短信验证码登录

```bash
# Step 1: 发送验证码
curl -X POST http://localhost:10000/api/v1/auth/credentials/verification-codes \
  -H "Content-Type: application/json" \
  -d '{
    "type": "sms",
    "recipient": "13800138000",
    "purpose": "login"
  }'

# Step 2: 使用验证码登录
curl -X POST http://localhost:10000/api/v1/auth/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "sms_code",
    "phone": "13800138000",
    "code": "123456",
    "clientId": "android",
    "autoRegister": true
  }'
```

### 3. OAuth2登录（GitHub示例）

```bash
# Step 1: 获取授权URL
curl -X GET http://localhost:10000/api/v1/auth/oauth/providers/github/authorization-url

# 响应: {"authorizationUrl": "https://github.com/login/oauth/authorize?...", "state": "xxx"}

# Step 2: 用户在浏览器中打开authorizationUrl授权

# Step 3: 授权后获取code，调用回调接口
curl -X POST http://localhost:10000/api/v1/auth/oauth/callback \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "github",
    "code": "4/0AY0e-g7...",
    "state": "xxx"
  }'
```

### 4. 微信扫码登录

```bash
# Step 1: 获取二维码
curl -X GET http://localhost:10000/api/v1/auth/oauth/providers/wechat/authorization-url

# Step 2: 前端显示二维码，用户扫码

# Step 3: 扫码后调用登录接口
curl -X POST http://localhost:10000/api/v1/auth/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "wechat_qrcode",
    "code": "wx_code_xxx",
    "state": "xxx",
    "clientId": "web"
  }'
```

### 5. 刷新Token

```bash
curl -X PUT http://localhost:10000/api/v1/auth/sessions/current \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "refreshToken": "refresh_token_xyz789"
  }'
```

### 6. 登出

```bash
curl -X DELETE http://localhost:10000/api/v1/auth/sessions/current \
  -H "Authorization: Bearer <accessToken>"
```

### 7. 多设备管理

```bash
# 查看所有设备
curl -X GET http://localhost:10000/api/v1/auth/sessions?userId=123456 \
  -H "Authorization: Bearer <accessToken>"

# 踢出指定设备
curl -X DELETE http://localhost:10000/api/v1/auth/sessions/session_xyz \
  -H "Authorization: Bearer <accessToken>"
```

---

## 🔐 安全特性

### 1. Token黑名单机制

```java

@Component
public class TokenBlacklistService {

    private final RedisService redisService;

    public void addToBlacklist(String token, Duration ttl) {
        String key = "token:blacklist:" + token;
        redisService.set(key, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        String key = "token:blacklist:" + token;
        return redisService.exists(key);
    }
}
```

### 2. CSRF防护

```java

@Component
public class CsrfTokenValidator {

    private final RedisService redisService;

    public String generateState() {
        String state = UUID.randomUUID().toString();
        String sessionId = SecurityUtils.getCurrentSessionId();

        String key = "csrf:state:" + state;
        redisService.set(key, sessionId, Duration.ofMinutes(10));

        return state;
    }

    public void validateState(String state) {
        String key = "csrf:state:" + state;
        String storedSessionId = redisService.get(key);

        if (storedSessionId == null) {
            throw new CsrfTokenInvalidException("State已过期或不存在");
        }

        String currentSessionId = SecurityUtils.getCurrentSessionId();
        if (!storedSessionId.equals(currentSessionId)) {
            throw new CsrfTokenInvalidException("State验证失败");
        }

        // 验证成功后删除
        redisService.delete(key);
    }
}
```

### 3. 限流策略

```java

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter authenticationRateLimiter() {
        return RateLimiter.builder()
                .name("authentication")
                .maxRequests(5)
                .window(Duration.ofMinutes(1))
                .identifier(RateLimitIdentifier.IP)
                .blockDuration(Duration.ofMinutes(30))
                .build();
    }
}

// 在Controller中使用
@PostMapping
@RateLimit(limiter = "authentication")
public R<AuthSession> createSession(...) {
    // ...
}
```

### 4. 审计日志

```java

@Aspect
@Component
public class AuthenticationAuditAspect {

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        String userId = SecurityUtils.getCurrentUserIdOrNull();
        String ip = HttpUtils.getClientIp();
        String device = HttpUtils.getUserAgent();
        long startTime = System.currentTimeMillis();

        try {
            Object result = point.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[AUTH_AUDIT] operation={}, userId={}, ip={}, device={}, duration={}ms, result=SUCCESS",
                    auditLog.operation(), userId, ip, device, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[AUTH_AUDIT] operation={}, userId={}, ip={}, device={}, duration={}ms, result=FAILED, error={}",
                    auditLog.operation(), userId, ip, device, duration, e.getMessage());

            throw e;
        }
    }
}
```

---

## 📊 监控指标

### 1. Metrics收集

```java

@Component
public class AuthenticationMetrics {

    private final MeterRegistry registry;

    // 登录成功率
    public void recordLogin(String grantType, boolean success) {
        Counter.builder("auth.login")
                .tag("grant_type", grantType)
                .tag("success", String.valueOf(success))
                .register(registry)
                .increment();
    }

    // 登录耗时
    public void recordLoginDuration(String grantType, long duration) {
        Timer.builder("auth.login.duration")
                .tag("grant_type", grantType)
                .register(registry)
                .record(Duration.ofMillis(duration));
    }

    // 活跃会话数
    public void recordActiveSessions(int count) {
        Gauge.builder("auth.sessions.active", () -> count)
                .register(registry);
    }

    // Token刷新次数
    public void recordTokenRefresh() {
        Counter.builder("auth.token.refresh")
                .register(registry)
                .increment();
    }
}
```

### 2. 监控面板

建议使用Grafana监控以下指标：

- 登录成功率（按认证方式）
- 平均登录耗时
- 活跃会话数
- Token刷新频率
- 登录失败原因分布
- OAuth2各提供商使用情况

---

## ✅ 验收标准

### 功能完整性

- [ ] 支持所有现有的认证方式
- [ ] 所有接口符合RESTful规范
- [ ] API文档完整（Swagger/OpenAPI）
- [ ] 错误处理统一规范

### 性能要求

- [ ] 登录接口响应时间 < 500ms (P99)
- [ ] 并发支持 1000 TPS
- [ ] Token验证 < 10ms

### 安全要求

- [ ] 实现Token黑名单
- [ ] 实现CSRF防护
- [ ] 实现限流保护
- [ ] 完整的审计日志

### 代码质量

- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试覆盖核心流程
- [ ] 代码符合阿里巴巴Java开发规范
- [ ] 通过SonarQube代码扫描

---

## 📚 参考文档

1. [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
2. [RESTful API设计最佳实践](https://restfulapi.net/)
3. [Spring Security官方文档](https://spring.io/projects/spring-security)
4. [JWT最佳实践](https://tools.ietf.org/html/rfc8725)

---

## 🎉 总结

本次重构完成后，认证服务将具备：

✅ **工业级架构** - 完全符合RESTful和OAuth 2.0标准
✅ **高扩展性** - 新增认证方式无需修改Controller
✅ **统一入口** - 所有认证方式统一API
✅ **优雅设计** - 使用策略模式、多态、模板方法等设计模式
✅ **安全可靠** - 完善的安全机制和监控体系
✅ **易于维护** - 职责清晰、代码简洁

**下一步**: 按照本指南完成剩余的Controller和Service实现！

