# 认证/注册模块重构技术规范文档

**项目**: Zing Framework Authentication Service  
**版本**: v2.0  
**日期**: 2025-11-05  
**负责人**: Architecture Team  
**文档级别**: 工业级架构设计规范

---

## 📋 目录

1. [执行摘要](#执行摘要)
2. [现状分析](#现状分析)
3. [问题清单](#问题清单)
4. [架构设计原则](#架构设计原则)
5. [重构方案](#重构方案)
6. [详细设计](#详细设计)
7. [实施计划](#实施计划)
8. [风险评估](#风险评估)

---

## 📊 执行摘要

### 当前状态
经过代码审查，认证服务存在**多套并行API、职责混乱、缺乏统一标准**等严重架构问题。虽然v1版本设计理念先进，但与旧版本共存导致维护困难。

### 核心问题
- ✗ **多版本API并存**：3套Controller实现（v1、旧版、混合版）
- ✗ **职责边界不清**：认证、注册、令牌管理职责混乱
- ✗ **代码重复**：相似逻辑在多处实现
- ✗ **缺乏统一异常处理**：错误处理不一致
- ✗ **安全机制不完善**：缺乏统一的审计、限流、监控
- ✗ **测试覆盖不足**：缺少集成测试和安全测试

### 重构目标
✓ **统一API版本**：废弃旧版，全面迁移至v1  
✓ **清晰的职责划分**：遵循单一职责原则  
✓ **企业级安全**：完善的审计、限流、加密机制  
✓ **高可扩展性**：插件化认证策略  
✓ **完整测试覆盖**：单元测试、集成测试、安全测试  

---

## 🔍 现状分析

### 1. Controller层分析

#### 问题1: 多套API并存（严重）
```
发现的Controller：
├── v1/AuthenticationController.java    ✓ 设计良好（新版）
├── v1/RegistrationController.java      ✓ 设计良好（新版）
├── v1/TokenController.java             ✓ 设计良好（新版）
├── v1/CredentialController.java        ✓ 设计良好（新版）
├── v1/OAuthProviderController.java     ✓ 设计良好（新版）
├── v1/AccountBindingController.java    ✓ 设计良好（新版）
├── LoginController.java                ✗ 旧版实现
├── RegisterController.java             ✗ 旧版实现
├── AuthController.java                 ✗ 混合实现
├── OAuth2Controller.java               ✗ 旧版实现
└── WeChatOAuth2Controller.java         ✗ 旧版实现
```

**影响**：
- 客户端不知道该使用哪套API
- 维护成本翻倍
- 安全策略不统一
- 文档混乱

#### 问题2: URL路径不统一
```
v1版本：/api/v1/auth/*
旧版本：/auth/*
混合版：随意
```

**标准应该是**：
```
/api/v1/auth/sessions              # 会话管理
/api/v1/auth/registrations         # 注册
/api/v1/auth/tokens                # 令牌管理
/api/v1/auth/credentials           # 凭证管理
/api/v1/auth/oauth/providers       # OAuth管理
/api/v1/auth/bindings              # 账户绑定
```

### 2. Service层分析

#### 问题3: Service实现混乱
```
发现的Service实现：
├── v1/impl/RegistrationServiceImpl.java       ✓ 新版
├── v1/impl/TokenServiceImpl.java              ✓ 新版
├── v1/impl/AccountBindingServiceImpl.java     ✓ 新版
├── impl/LoginServiceImpl.java                 ✗ 旧版
├── impl/RegisterServiceImpl.java              ✗ 旧版
├── impl/SignInOrRegisterServiceImpl.java      ✗ 功能重复
├── impl/AuthVerificationCodeServiceImpl.java  ✗ 职责不清
└── AuthenticationService.java                 ? 缺少实现类
```

**问题**：
- `AuthenticationService`接口存在但找不到实现类
- `SignInOrRegisterService`与注册/登录逻辑重复
- 验证码服务职责混乱（既在Touch又在Auth）
- 缺少统一的认证策略抽象

#### 问题4: 缺少统一的认证策略模式
虽然v1设计使用了策略模式（`CreateSessionRequest`的多态），但实现不完整：
```java
// 当前设计（好的思路）
@JsonSubTypes({
    @JsonSubTypes.Type(value = PasswordGrantRequest.class, name = "password"),
    @JsonSubTypes.Type(value = SmsCodeGrantRequest.class, name = "sms_code"),
    // ...
})
public abstract class CreateSessionRequest {
    public abstract AuthSession authenticate(AuthenticationService service);
}
```

**缺失**：
- 没有独立的`AuthenticationStrategy`接口
- 没有统一的策略注册机制
- 扩展新认证方式需要修改多处代码
- 缺少认证前置/后置处理器（日志、审计、限流）

### 3. 安全机制分析

#### 问题5: 安全机制不完善
```java
// ✓ 有的安全特性
- JWT Token生成和验证（JwtTokenProvider）
- 密码加密（BCryptPasswordEncoder）
- 防重放攻击（@PreventResubmit）
- Token黑名单（JwtTokenStore）

// ✗ 缺失的安全特性
- 统一的认证审计日志
- 失败锁定机制（账户暴力破解保护）
- 设备指纹识别
- 异常登录检测
- 密码强度策略配置
- 敏感操作二次验证
- 完整的RBAC权限模型
```

#### 问题6: SecurityAutoConfiguration配置不灵活
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    // 问题：白名单硬编码在配置中
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(securityProperties.getWhitelist().toArray(new String[0]))
        .permitAll()
        .anyRequest().authenticated()
    )
}
```

**问题**：
- 白名单配置不够灵活
- 缺少动态权限控制
- 缺少API级别的限流配置
- 缺少会话并发控制

### 4. Framework自动配置分析

#### 问题7: RedisAutoConfiguration设计不足
```java
// 当前实现
@Bean
public RedisTemplate<String, Object> potRedisTemplate(...)
```

**问题**：
- 缺少分布式锁支持（认证场景需要）
- 缺少缓存降级机制
- 序列化配置不够灵活
- 缺少缓存预热机制

#### 问题8: TouchAutoConfiguration职责混乱
```java
// Touch框架既负责验证码又负责消息推送
@Bean
public VerificationCodeService verificationCodeService(...)
```

**问题**：
- 验证码服务应该属于Auth领域，不应该在Touch
- Touch应该专注于"触达"（发送消息），不应该管理业务逻辑
- 导致Auth服务对Touch的强依赖

### 5. 数据模型分析

#### 问题9: 缺少领域模型
当前只有DTO，缺少领域模型：
```
✗ 缺少 User/Member 领域模型
✗ 缺少 Session 领域模型
✗ 缺少 Credential 领域模型
✗ 缺少 AuthenticationAttempt 审计模型
```

这导致：
- 业务逻辑分散在Service层
- 缺少领域不变性约束
- 难以实现复杂的业务规则

#### 问题10: DTO设计不一致
```java
// v1版本
AuthSession (包含UserInfo、TokenInfo、DeviceInfo)

// 旧版本
AuthResponse (包含token、userInfo)

// 问题：
- 命名不统一（Response vs Session）
- 字段不一致
- 缺少版本控制
```

---

## ❌ 问题清单（优先级排序）

### P0 - 严重问题（必须立即解决）
1. **多套API并存** - 导致客户端困惑和维护灾难
2. **AuthenticationService缺少实现** - 核心服务不完整
3. **安全审计缺失** - 无法追踪认证行为
4. **统一异常处理缺失** - 错误响应不一致

### P1 - 高优先级（影响可扩展性）
5. **认证策略不完整** - 难以扩展新认证方式
6. **验证码服务职责混乱** - Touch与Auth耦合
7. **缺少失败锁定机制** - 安全风险
8. **Session管理不完善** - 多设备支持不足

### P2 - 中优先级（影响可维护性）
9. **Service层职责混乱** - 代码重复
10. **缺少领域模型** - 业务逻辑分散
11. **DTO设计不一致** - 接口混乱
12. **配置不够灵活** - 难以适配不同场景

### P3 - 低优先级（优化项）
13. **缺少缓存预热** - 启动性能
14. **缺少监控指标** - 运维可观测性
15. **测试覆盖不足** - 质量保证

---

## 🏗️ 架构设计原则

### 1. SOLID原则
- **S**ingle Responsibility: 每个类只负责一个职责
- **O**pen/Closed: 对扩展开放，对修改关闭
- **L**iskov Substitution: 子类可替换父类
- **I**nterface Segregation: 接口隔离
- **D**ependency Inversion: 依赖倒置

### 2. DDD（领域驱动设计）
```
认证领域核心概念：
├── Aggregate Root: User, Session
├── Entity: Credential, OAuthBinding
├── Value Object: DeviceInfo, TokenInfo
├── Domain Service: AuthenticationService, CredentialService
├── Repository: UserRepository, SessionRepository
└── Domain Event: UserRegistered, UserLoggedIn, PasswordChanged
```

### 3. Clean Architecture
```
┌─────────────────────────────────────┐
│     Presentation Layer (API)        │  Controller
├─────────────────────────────────────┤
│     Application Layer               │  Service (Use Cases)
├─────────────────────────────────────┤
│     Domain Layer                    │  Domain Model, Domain Service
├─────────────────────────────────────┤
│     Infrastructure Layer            │  Repository, External Services
└─────────────────────────────────────┘
```

### 4. 安全优先
- 零信任架构
- 最小权限原则
- 深度防御
- 安全审计

---

## 🔧 重构方案

### 阶段1: 废弃旧版API（Week 1-2）

#### 1.1 标记废弃
```java
@Deprecated(since = "2.0", forRemoval = true)
@RestController
@RequestMapping("/auth")
public class LoginController {
    // 添加废弃警告
    @PostMapping("/login")
    public R<AuthResponse> login(...) {
        log.warn("使用了已废弃的API: /auth/login，请迁移至 /api/v1/auth/sessions");
        // 内部转发到新API
        return redirectToNewApi(...);
    }
}
```

#### 1.2 提供迁移指南
创建 `API_MIGRATION_GUIDE.md` 文档

#### 1.3 监控旧API使用情况
```java
@Aspect
public class DeprecatedApiMonitorAspect {
    @Around("@within(Deprecated)")
    public Object monitor(ProceedingJoinPoint pjp) {
        // 记录到监控系统
        metrics.incrementDeprecatedApiCall(pjp.getSignature());
        return pjp.proceed();
    }
}
```

### 阶段2: 完善认证策略模式（Week 3-4）

#### 2.1 定义统一的认证策略接口
```java
/**
 * 认证策略接口
 * 所有认证方式必须实现此接口
 */
public interface AuthenticationStrategy {
    
    /**
     * 认证类型标识
     */
    String getGrantType();
    
    /**
     * 执行认证
     * @param context 认证上下文
     * @return 认证结果
     */
    AuthenticationResult authenticate(AuthenticationContext context);
    
    /**
     * 验证认证请求
     */
    void validate(AuthenticationRequest request);
    
    /**
     * 是否支持自动注册
     */
    default boolean supportsAutoRegister() {
        return false;
    }
}
```

#### 2.2 实现策略管理器
```java
@Component
public class AuthenticationStrategyManager {
    
    private final Map<String, AuthenticationStrategy> strategies;
    
    public AuthenticationStrategyManager(List<AuthenticationStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                AuthenticationStrategy::getGrantType,
                Function.identity()
            ));
    }
    
    public AuthenticationStrategy getStrategy(String grantType) {
        AuthenticationStrategy strategy = strategies.get(grantType);
        if (strategy == null) {
            throw new UnsupportedGrantTypeException(grantType);
        }
        return strategy;
    }
    
    public Set<String> getSupportedGrantTypes() {
        return strategies.keySet();
    }
}
```

#### 2.3 实现具体策略
```java
@Component
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountLockService accountLockService;
    
    @Override
    public String getGrantType() {
        return "password";
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationContext context) {
        PasswordGrantRequest request = (PasswordGrantRequest) context.getRequest();
        
        // 1. 检查账户锁定状态
        accountLockService.checkLocked(request.getUsername());
        
        // 2. 查找用户
        User user = userRepository.findByUsernameOrEmailOrPhone(request.getUsername())
            .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));
        
        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            accountLockService.recordFailure(user.getId());
            throw new BadCredentialsException("用户名或密码错误");
        }
        
        // 4. 重置失败计数
        accountLockService.resetFailureCount(user.getId());
        
        // 5. 返回认证结果
        return AuthenticationResult.success(user);
    }
    
    @Override
    public void validate(AuthenticationRequest request) {
        PasswordGrantRequest req = (PasswordGrantRequest) request;
        Assert.hasText(req.getUsername(), "用户名不能为空");
        Assert.hasText(req.getPassword(), "密码不能为空");
    }
}
```

### 阶段3: 实现完整的AuthenticationService（Week 5-6）

#### 3.1 核心Service实现
```java
@Service
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final AuthenticationStrategyManager strategyManager;
    private final SessionManager sessionManager;
    private final TokenGenerator tokenGenerator;
    private final AuthenticationAuditService auditService;
    private final List<AuthenticationInterceptor> interceptors;
    
    @Override
    @Transactional
    public AuthSession createSession(CreateSessionRequest request) {
        // 1. 前置拦截器
        AuthenticationContext context = new AuthenticationContext(request);
        executeInterceptors(interceptors, context, InterceptorPhase.PRE_AUTH);
        
        try {
            // 2. 获取认证策略
            AuthenticationStrategy strategy = strategyManager.getStrategy(
                request.getGrantType()
            );
            
            // 3. 验证请求
            strategy.validate(request);
            
            // 4. 执行认证
            AuthenticationResult result = strategy.authenticate(context);
            
            // 5. 生成会话
            AuthSession session = sessionManager.createSession(
                result.getUser(),
                request.getClientId(),
                request.getDeviceInfo()
            );
            
            // 6. 生成令牌
            TokenPair tokenPair = tokenGenerator.generate(session);
            session.setTokenInfo(tokenPair);
            
            // 7. 后置拦截器
            executeInterceptors(interceptors, context, InterceptorPhase.POST_AUTH);
            
            // 8. 审计日志
            auditService.recordSuccess(session, request);
            
            return session;
            
        } catch (AuthenticationException e) {
            // 审计失败
            auditService.recordFailure(request, e);
            throw e;
        }
    }
    
    // 其他方法...
}
```

#### 3.2 实现拦截器机制
```java
public interface AuthenticationInterceptor extends Ordered {
    
    void preAuthenticate(AuthenticationContext context);
    
    void postAuthenticate(AuthenticationContext context);
    
    void onAuthenticationFailure(AuthenticationContext context, Exception e);
}

@Component
@Order(100)
public class RateLimitInterceptor implements AuthenticationInterceptor {
    
    private final RateLimitManager rateLimitManager;
    
    @Override
    public void preAuthenticate(AuthenticationContext context) {
        String key = buildKey(context);
        if (!rateLimitManager.tryAcquire(key)) {
            throw new RateLimitExceededException("登录请求过于频繁");
        }
    }
    
    private String buildKey(AuthenticationContext context) {
        // IP + GrantType
        return String.format("auth:ratelimit:%s:%s",
            context.getClientIp(),
            context.getRequest().getGrantType()
        );
    }
}

@Component
@Order(200)
public class DeviceFingerprintInterceptor implements AuthenticationInterceptor {
    
    @Override
    public void preAuthenticate(AuthenticationContext context) {
        DeviceInfo deviceInfo = context.getRequest().getDeviceInfo();
        if (deviceInfo != null) {
            // 设备指纹识别
            String fingerprint = generateFingerprint(deviceInfo);
            context.setAttribute("deviceFingerprint", fingerprint);
        }
    }
}

@Component
@Order(300)
public class AbnormalLoginDetectionInterceptor implements AuthenticationInterceptor {
    
    private final LoginBehaviorAnalyzer analyzer;
    
    @Override
    public void postAuthenticate(AuthenticationContext context) {
        // 异常登录检测
        if (analyzer.isAbnormal(context)) {
            // 发送警告通知
            notificationService.sendSecurityAlert(context.getUser());
        }
    }
}
```

### 阶段4: 实现安全增强（Week 7-8）

#### 4.1 账户锁定机制
```java
@Service
public class AccountLockService {
    
    private final RedisService redisService;
    private final SecurityProperties securityProperties;
    
    private static final String LOCK_KEY = "auth:lock:";
    private static final String FAILURE_KEY = "auth:failure:";
    
    /**
     * 检查账户是否被锁定
     */
    public void checkLocked(String identifier) {
        String lockKey = LOCK_KEY + identifier;
        if (redisService.hasKey(lockKey)) {
            Long ttl = redisService.getExpire(lockKey);
            throw new AccountLockedException(
                String.format("账户已被锁定，请在%d分钟后重试", ttl / 60)
            );
        }
    }
    
    /**
     * 记录失败尝试
     */
    public void recordFailure(String identifier) {
        String failureKey = FAILURE_KEY + identifier;
        Long failures = redisService.increment(failureKey);
        redisService.expire(failureKey, 1, TimeUnit.HOURS);
        
        // 超过阈值则锁定
        if (failures >= securityProperties.getMaxFailureAttempts()) {
            lockAccount(identifier);
        }
    }
    
    /**
     * 锁定账户
     */
    private void lockAccount(String identifier) {
        String lockKey = LOCK_KEY + identifier;
        redisService.set(
            lockKey,
            System.currentTimeMillis(),
            securityProperties.getLockDuration(),
            TimeUnit.MINUTES
        );
        
        // 发送通知
        eventPublisher.publish(new AccountLockedEvent(identifier));
    }
    
    /**
     * 重置失败计数
     */
    public void resetFailureCount(String identifier) {
        String failureKey = FAILURE_KEY + identifier;
        redisService.delete(failureKey);
    }
}
```

#### 4.2 审计日志
```java
@Service
public class AuthenticationAuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public void recordSuccess(AuthSession session, CreateSessionRequest request) {
        AuthAuditLog log = AuthAuditLog.builder()
            .userId(session.getUserInfo().getUserId())
            .username(session.getUserInfo().getUsername())
            .grantType(request.getGrantType())
            .clientId(request.getClientId())
            .clientIp(RequestContextHolder.getClientIp())
            .userAgent(RequestContextHolder.getUserAgent())
            .deviceInfo(request.getDeviceInfo())
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
        
        // 异步保存
        auditLogRepository.save(log);
        
        // 发布事件
        eventPublisher.publishEvent(new UserLoggedInEvent(session));
    }
    
    public void recordFailure(CreateSessionRequest request, Exception e) {
        AuthAuditLog log = AuthAuditLog.builder()
            .grantType(request.getGrantType())
            .clientId(request.getClientId())
            .clientIp(RequestContextHolder.getClientIp())
            .userAgent(RequestContextHolder.getUserAgent())
            .success(false)
            .failureReason(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        auditLogRepository.save(log);
    }
}
```

#### 4.3 密码策略
```java
@Component
public class PasswordPolicyValidator {
    
    private final SecurityProperties securityProperties;
    
    public void validate(String password) {
        PasswordPolicy policy = securityProperties.getPasswordPolicy();
        
        // 长度检查
        if (password.length() < policy.getMinLength() ||
            password.length() > policy.getMaxLength()) {
            throw new PasswordPolicyViolationException(
                String.format("密码长度必须在%d-%d位之间",
                    policy.getMinLength(), policy.getMaxLength())
            );
        }
        
        // 复杂度检查
        int complexity = 0;
        if (password.matches(".*[a-z].*")) complexity++;  // 小写字母
        if (password.matches(".*[A-Z].*")) complexity++;  // 大写字母
        if (password.matches(".*\\d.*")) complexity++;    // 数字
        if (password.matches(".*[!@#$%^&*].*")) complexity++; // 特殊字符
        
        if (complexity < policy.getRequiredComplexity()) {
            throw new PasswordPolicyViolationException(
                "密码必须包含大写字母、小写字母、数字和特殊字符"
            );
        }
        
        // 常见密码检查
        if (policy.isCheckCommonPasswords() && 
            CommonPasswordChecker.isCommon(password)) {
            throw new PasswordPolicyViolationException(
                "密码过于简单，请使用更复杂的密码"
            );
        }
    }
}
```

### 阶段5: 重构验证码服务（Week 9）

#### 5.1 从Touch中分离验证码业务逻辑
```java
// Auth模块
@Service
public class VerificationCodeManager {
    
    private final VerificationCodeRepository repository;
    private final TouchService touchService;  // 只依赖发送能力
    private final RedisService redisService;
    
    /**
     * 发送验证码
     */
    public void send(SendCodeRequest request) {
        // 1. 生成验证码
        String code = generateCode();
        
        // 2. 保存到缓存
        String cacheKey = buildCacheKey(request.getRecipient(), request.getPurpose());
        redisService.set(cacheKey, code, 5, TimeUnit.MINUTES);
        
        // 3. 调用Touch发送（只负责发送，不管业务）
        if ("sms".equals(request.getType())) {
            touchService.sendSms(request.getRecipient(), 
                "SMS_VERIFICATION_CODE", 
                Map.of("code", code));
        } else if ("email".equals(request.getType())) {
            touchService.sendEmail(request.getRecipient(),
                "EMAIL_VERIFICATION_CODE",
                Map.of("code", code));
        }
        
        // 4. 审计日志
        auditLog.info("验证码已发送: type={}, recipient={}, purpose={}",
            request.getType(), mask(request.getRecipient()), request.getPurpose());
    }
    
    /**
     * 验证验证码
     */
    public boolean verify(String recipient, String code, String purpose) {
        String cacheKey = buildCacheKey(recipient, purpose);
        String cachedCode = redisService.get(cacheKey);
        
        if (cachedCode == null) {
            return false;
        }
        
        boolean valid = cachedCode.equals(code);
        
        if (valid) {
            // 验证成功后删除
            redisService.delete(cacheKey);
        }
        
        return valid;
    }
}
```

#### 5.2 简化Touch框架职责
```java
// Touch只负责"触达"能力
public interface TouchService {
    
    /**
     * 发送短信
     */
    void sendSms(String phone, String templateCode, Map<String, Object> params);
    
    /**
     * 发送邮件
     */
    void sendEmail(String email, String templateCode, Map<String, Object> params);
    
    /**
     * 发送App推送
     */
    void sendPush(String userId, String title, String content);
    
    // 不再包含验证码相关的业务逻辑
}
```

### 阶段6: 统一异常处理（Week 10）

#### 6.1 定义异常体系
```java
// 认证异常基类
public abstract class AuthenticationException extends RuntimeException {
    private final String errorCode;
    private final Object[] args;
    
    protected AuthenticationException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
}

// 具体异常
public class BadCredentialsException extends AuthenticationException {
    public BadCredentialsException() {
        super("AUTH_001", "用户名或密码错误");
    }
}

public class AccountLockedException extends AuthenticationException {
    public AccountLockedException(String message) {
        super("AUTH_002", message);
    }
}

public class InvalidVerificationCodeException extends AuthenticationException {
    public InvalidVerificationCodeException() {
        super("AUTH_003", "验证码错误或已过期");
    }
}

public class UnsupportedGrantTypeException extends AuthenticationException {
    public UnsupportedGrantTypeException(String grantType) {
        super("AUTH_004", "不支持的认证类型: " + grantType, grantType);
    }
}
```

#### 6.2 全局异常处理器
```java
@RestControllerAdvice
@Slf4j
public class GlobalAuthExceptionHandler {
    
    private final MessageSource messageSource;
    
    @ExceptionHandler(AuthenticationException.class)
    public R<Void> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request) {
        
        log.warn("认证失败: errorCode={}, message={}, uri={}",
            e.getErrorCode(), e.getMessage(), request.getRequestURI());
        
        return R.fail(e.getErrorCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e) {
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return R.fail("VALIDATION_ERROR", "请求参数验证失败", errors);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public R<Void> handleRateLimitException(RateLimitExceededException e) {
        log.warn("请求频率超限: {}", e.getMessage());
        return R.fail("RATE_LIMIT_EXCEEDED", e.getMessage());
    }
}
```

### 阶段7: 完善配置体系（Week 11）

#### 7.1 增强SecurityProperties
```java
@ConfigurationProperties(prefix = "zing.security")
@Data
public class SecurityProperties {
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * JWT配置
     */
    private JwtConfig jwt = new JwtConfig();
    
    /**
     * 密码策略
     */
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    
    /**
     * 账户锁定配置
     */
    private AccountLockConfig accountLock = new AccountLockConfig();
    
    /**
     * 会话配置
     */
    private SessionConfig session = new SessionConfig();
    
    /**
     * OAuth配置
     */
    private Map<String, OAuthProviderConfig> oauth = new HashMap<>();
    
    /**
     * 白名单
     */
    private List<String> whitelist = new ArrayList<>();
    
    @Data
    public static class PasswordPolicy {
        private int minLength = 8;
        private int maxLength = 20;
        private int requiredComplexity = 3;
        private boolean checkCommonPasswords = true;
        private int passwordHistorySize = 5;  // 不能与最近N次密码相同
    }
    
    @Data
    public static class AccountLockConfig {
        private boolean enabled = true;
        private int maxFailureAttempts = 5;
        private int lockDuration = 30;  // 分钟
        private TimeUnit lockTimeUnit = TimeUnit.MINUTES;
    }
    
    @Data
    public static class SessionConfig {
        private int maxConcurrentSessions = 5;  // 最大并发会话数
        private boolean preventConcurrentLogin = false;  // 是否阻止并发登录
        private int accessTokenExpiration = 3600;  // 秒
        private int refreshTokenExpiration = 604800;  // 秒
    }
}
```

#### 7.2 配置示例
```yaml
zing:
  security:
    enabled: true
    
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      issuer: zing-auth
      header: Authorization
      prefix: "Bearer "
      
    password-policy:
      min-length: 8
      max-length: 20
      required-complexity: 3
      check-common-passwords: true
      password-history-size: 5
      
    account-lock:
      enabled: true
      max-failure-attempts: 5
      lock-duration: 30
      lock-time-unit: MINUTES
      
    session:
      max-concurrent-sessions: 5
      prevent-concurrent-login: false
      access-token-expiration: 3600
      refresh-token-expiration: 604800
      
    oauth:
      github:
        client-id: ${GITHUB_CLIENT_ID}
        client-secret: ${GITHUB_CLIENT_SECRET}
        redirect-uri: https://yourdomain.com/oauth2/callback/github
      google:
        client-id: ${GOOGLE_CLIENT_ID}
        client-secret: ${GOOGLE_CLIENT_SECRET}
        redirect-uri: https://yourdomain.com/oauth2/callback/google
        
    whitelist:
      - /api/v1/auth/sessions
      - /api/v1/auth/registrations
      - /api/v1/auth/tokens/refresh
      - /api/v1/auth/credentials/verification-codes
      - /api/v1/auth/oauth/**
      - /swagger-ui/**
      - /v3/api-docs/**
```

---

## 📐 详细设计

### 1. 最终目录结构
```
auth-service/
├── api/                                    # API层
│   └── controller/
│       └── v1/
│           ├── AuthenticationController    ✓ 保留
│           ├── RegistrationController      ✓ 保留
│           ├── TokenController             ✓ 保留
│           ├── CredentialController        ✓ 保留
│           ├── OAuthProviderController     ✓ 保留
│           └── AccountBindingController    ✓ 保留
├── application/                            # 应用层
│   ├── service/
│   │   ├── AuthenticationServiceImpl       ✓ 新增
│   │   ├── RegistrationServiceImpl         ✓ 已存在
│   │   ├── TokenServiceImpl                ✓ 已存在
│   │   ├── CredentialServiceImpl           ✓ 新增
│   │   └── OAuthProviderServiceImpl        ✓ 新增
│   ├── strategy/                           # 认证策略
│   │   ├── AuthenticationStrategy          ✓ 新增接口
│   │   ├── PasswordAuthenticationStrategy  ✓ 新增
│   │   ├── SmsCodeAuthenticationStrategy   ✓ 新增
│   │   ├── EmailCodeAuthenticationStrategy ✓ 新增
│   │   ├── OAuth2AuthenticationStrategy    ✓ 新增
│   │   └── WeChatAuthenticationStrategy    ✓ 新增
│   └── interceptor/                        # 拦截器
│       ├── RateLimitInterceptor            ✓ 新增
│       ├── DeviceFingerprintInterceptor    ✓ 新增
│       └── AbnormalLoginDetectionInterceptor ✓ 新增
├── domain/                                 # 领域层
│   ├── model/
│   │   ├── User                            ✓ 新增
│   │   ├── Session                         ✓ 新增
│   │   ├── Credential                      ✓ 新增
│   │   └── OAuthBinding                    ✓ 新增
│   ├── service/                            # 领域服务
│   │   ├── AccountLockService              ✓ 新增
│   │   ├── PasswordPolicyValidator         ✓ 新增
│   │   └── SessionManager                  ✓ 新增
│   ├── repository/                         # 仓储接口
│   │   ├── UserRepository
│   │   ├── SessionRepository
│   │   └── AuditLogRepository
│   └── event/                              # 领域事件
│       ├── UserRegisteredEvent
│       ├── UserLoggedInEvent
│       └── PasswordChangedEvent
├── infrastructure/                         # 基础设施层
│   ├── repository/                         # 仓储实现
│   │   └── impl/
│   ├── security/
│   │   ├── JwtTokenProvider
│   │   ├── JwtTokenStore
│   │   └── MemberUserDetailsService
│   └── audit/
│       └── AuthenticationAuditService
└── dto/                                    # 数据传输对象
    ├── request/
    └── response/
```

### 2. 核心类图
```
┌─────────────────────────────────────────────────────┐
│         AuthenticationController                    │
├─────────────────────────────────────────────────────┤
│ + createSession(CreateSessionRequest): AuthSession  │
│ + refreshSession(RefreshSessionRequest): AuthSession│
│ + destroySession(): void                            │
└────────────┬────────────────────────────────────────┘
             │ uses
             ▼
┌─────────────────────────────────────────────────────┐
│         AuthenticationService                       │
├─────────────────────────────────────────────────────┤
│ + createSession(request): AuthSession               │
│ + refreshSession(sessionId, token): AuthSession     │
│ + destroySession(sessionId): void                   │
└────────────┬────────────────────────────────────────┘
             │ uses
             ▼
┌─────────────────────────────────────────────────────┐
│      AuthenticationStrategyManager                  │
├─────────────────────────────────────────────────────┤
│ - strategies: Map<String, AuthenticationStrategy>   │
│ + getStrategy(grantType): AuthenticationStrategy    │
└────────────┬────────────────────────────────────────┘
             │ manages
             ▼
┌─────────────────────────────────────────────────────┐
│      <<interface>> AuthenticationStrategy           │
├─────────────────────────────────────────────────────┤
│ + getGrantType(): String                            │
│ + authenticate(context): AuthenticationResult       │
│ + validate(request): void                           │
└────────────┬────────────────────────────────────────┘
             │ implements
             ├────────────────┬─────────────┬──────────┐
             ▼                ▼             ▼          ▼
    ┌────────────────┐  ┌─────────┐  ┌─────────┐  ┌──────┐
    │ Password       │  │SmsCode  │  │EmailCode│  │OAuth2│
    │ Strategy       │  │Strategy │  │Strategy │  │Strat │
    └────────────────┘  └─────────┘  └─────────┘  └──────┘
```

### 3. 认证流程序列图
```
Client         Controller        Service         Strategy        Repository
  │                │                │                │                │
  ├─POST /sessions─>│                │                │                │
  │                ├─createSession─>│                │                │
  │                │                ├─getStrategy───>│                │
  │                │                │<───return──────┤                │
  │                │                ├─validate──────>│                │
  │                │                │                ├─findUser──────>│
  │                │                │                │<───User────────┤
  │                │                │                ├─checkPassword  │
  │                │                │<───Result──────┤                │
  │                │                ├─createSession  │                │
  │                │                ├─generateToken  │                │
  │                │                ├─recordAudit    │                │
  │                │<───Session─────┤                │                │
  │<───Response────┤                │                │                │
```

### 4. 数据模型设计
```sql
-- 用户表（由Member服务管理）
CREATE TABLE member (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 会话表
CREATE TABLE auth_session (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_id VARCHAR(50),
    grant_type VARCHAR(50),
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    device_fingerprint VARCHAR(255),
    device_info JSON,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_access_token (access_token(255)),
    INDEX idx_refresh_token (refresh_token(255))
);

-- 审计日志表
CREATE TABLE auth_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    grant_type VARCHAR(50),
    client_id VARCHAR(50),
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    device_info JSON,
    success BOOLEAN,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_success (success)
);

-- OAuth绑定表
CREATE TABLE oauth_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    open_id VARCHAR(255) NOT NULL,
    union_id VARCHAR(255),
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    expires_at TIMESTAMP,
    profile JSON,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE KEY uk_provider_openid (provider, open_id),
    INDEX idx_user_id (user_id)
);

-- 密码历史表（用于密码策略）
CREATE TABLE password_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    INDEX idx_user_id (user_id)
);
```

---

## 📅 实施计划

### 时间表（12周）
```
Week 1-2:   废弃旧版API，添加迁移指南
Week 3-4:   实现认证策略模式
Week 5-6:   实现AuthenticationService
Week 7-8:   安全增强（锁定、审计）
Week 9:     重构验证码服务
Week 10:    统一异常处理
Week 11:    完善配置体系
Week 12:    测试、文档、上线
```

### 里程碑
- **M1** (Week 2): 旧版API标记废弃，监控建立
- **M2** (Week 4): 策略模式实现完成
- **M3** (Week 6): 核心Service实现完成
- **M4** (Week 8): 安全机制完善
- **M5** (Week 10): 异常处理统一
- **M6** (Week 12): 全面上线

### 人力分配
- **架构师** 1人：设计review、难点攻关
- **后端开发** 2-3人：核心功能实现
- **测试工程师** 1人：测试用例、自动化测试
- **运维工程师** 1人：监控、部署

---

## ⚠️ 风险评估

### 技术风险
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 新旧API共存期间数据不一致 | 高 | 中 | 1. 新旧API共享数据层<br>2. 数据库事务保证<br>3. 灰度发布 |
| 性能下降 | 中 | 低 | 1. 压力测试<br>2. 缓存优化<br>3. 异步处理 |
| 安全漏洞 | 高 | 低 | 1. 安全review<br>2. 渗透测试<br>3. 安全扫描 |

### 业务风险
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 客户端迁移不及时 | 中 | 高 | 1. 提前3个月通知<br>2. 详细迁移文档<br>3. 技术支持 |
| 用户体验中断 | 高 | 低 | 1. 灰度发布<br>2. 快速回滚机制<br>3. 监控告警 |

### 风险应对
1. **灰度发布策略**
   - Week 1-2: 5%流量
   - Week 3-4: 20%流量
   - Week 5-6: 50%流量
   - Week 7+: 100%流量

2. **快速回滚**
   - 保留旧版本代码
   - Feature Toggle控制
   - 数据库兼容设计

3. **监控告警**
   - 错误率监控
   - 性能监控
   - 业务指标监控

---

## ✅ 验收标准

### 功能验收
- [ ] 所有v1 API正常工作
- [ ] 所有认证方式可用（密码、短信、邮箱、OAuth2）
- [ ] 会话管理完整（创建、刷新、销毁、多设备）
- [ ] 注册流程完整（验证、注册、自动登录）
- [ ] 密码管理完整（修改、重置、策略验证）
- [ ] OAuth2绑定管理完整

### 性能验收
- [ ] 登录接口P99响应时间 < 200ms
- [ ] 注册接口P99响应时间 < 500ms
- [ ] 支持1000 QPS并发
- [ ] Redis缓存命中率 > 95%

### 安全验收
- [ ] 通过OWASP Top 10安全检查
- [ ] 密码加密强度符合标准
- [ ] 审计日志完整
- [ ] 账户锁定机制有效
- [ ] 异常登录检测准确

### 代码质量验收
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试覆盖核心场景
- [ ] SonarQube代码评分 > A
- [ ] 无Critical/Blocker级别问题

### 文档验收
- [ ] API文档完整（Swagger）
- [ ] 架构文档完整
- [ ] 迁移指南完整
- [ ] 运维手册完整

---

## 📚 附录

### A. API迁移对照表
| 旧API | 新API | 说明 |
|-------|-------|------|
| POST /auth/login | POST /api/v1/auth/sessions | 统一登录入口 |
| POST /auth/register | POST /api/v1/auth/registrations | 用户注册 |
| POST /auth/logout | DELETE /api/v1/auth/sessions/current | 登出 |
| POST /auth/refresh | PUT /api/v1/auth/sessions/current | 刷新会话 |

### B. 错误码对照表
| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| AUTH_001 | 用户名或密码错误 | 401 |
| AUTH_002 | 账户已锁定 | 423 |
| AUTH_003 | 验证码错误 | 400 |
| AUTH_004 | 不支持的认证类型 | 400 |
| AUTH_005 | Token已过期 | 401 |
| AUTH_006 | Token无效 | 401 |
| AUTH_007 | 密码策略不符 | 400 |

### C. 监控指标
```
# 业务指标
auth_login_total                    # 登录总数
auth_login_success_total            # 登录成功数
auth_login_failure_total            # 登录失败数
auth_register_total                 # 注册总数
auth_session_active_count           # 活跃会话数
auth_account_locked_total           # 账户锁定数

# 性能指标
auth_api_duration_seconds           # API响应时间
auth_db_query_duration_seconds      # 数据库查询时间
auth_cache_hit_rate                 # 缓存命中率

# 安全指标
auth_abnormal_login_total           # 异常登录数
auth_rate_limit_exceeded_total      # 限流触发数
```

### D. 参考资料
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design by Eric Evans](https://domainlanguage.com/ddd/)

---

## 📝 文档变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2025-11-05 | Architecture Team | 初始版本 |

---

**审批签字**：

架构师：____________  日期：________

技术负责人：____________  日期：________

项目经理：____________  日期：________

---

*本文档为机密文档，仅供内部使用*

