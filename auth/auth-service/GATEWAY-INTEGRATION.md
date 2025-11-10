# Gateway与Auth-Service集成架构设计

> **目标**: 网关层承担认证鉴权限流职责，Auth-Service作为认证授权中心

---

## 📐 架构原则

### 1. 职责划分

| 组件               | 职责                                                            | 性能要求   |
|------------------|---------------------------------------------------------------|--------|
| **Gateway**      | - JWT Token本地验证 (不调用服务)<br>- 权限预检查 (基于缓存)<br>- 限流控制<br>- 路由转发 | <10ms  |
| **Auth-Service** | - 用户认证 (登录)<br>- Token签发<br>- Token刷新<br>- 权限管理<br>- 权限变更通知   | <200ms |

### 2. 核心设计

```
┌──────────────────────────────────────────────────────────┐
│                      Client                              │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ 1. 携带JWT Token
                 ▼
┌─────────────────────────────────────────────────────────┐
│                    Gateway                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Step 1: JWT Token本地验证                         │ │
│  │  - 签名验证 (使用公钥/密钥)                         │ │
│  │  - 过期时间检查                                     │ │
│  │  - 黑名单检查 (Redis本地缓存)                      │ │
│  │  ❌ 验证失败 → 返回401                             │ │
│  └────────────────────────────────────────────────────┘ │
│                        │                                 │
│                        ▼                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Step 2: 权限预检查 (可选)                          │ │
│  │  - 从Token解析用户权限                              │ │
│  │  - 检查Redis权限缓存                                │ │
│  │  - 简单路径权限匹配                                 │ │
│  │  ❌ 无权限 → 返回403                               │ │
│  └────────────────────────────────────────────────────┘ │
│                        │                                 │
│                        ▼                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Step 3: 限流控制                                   │ │
│  │  - 基于用户/IP的限流                                │ │
│  │  - Redis计数器                                      │ │
│  │  ❌ 超限 → 返回429                                 │ │
│  └────────────────────────────────────────────────────┘ │
│                        │                                 │
│                        ▼                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Step 4: 转发请求                                   │ │
│  │  - 添加用户上下文Header                             │ │
│  │  - X-User-Id, X-User-Domain, X-Authorities         │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
         ┌────────────────┐
         │ Backend Service│
         │ (member-service│
         │  等业务服务)   │
         └────────────────┘


┌─────────────────────────────────────────────────────────┐
│                Auth-Service                              │
│  ┌────────────────────────────────────────────────────┐ │
│  │  核心API                                            │ │
│  │  1. POST /auth/login     - 用户登录                │ │
│  │  2. POST /auth/refresh   - 刷新Token               │ │
│  │  3. POST /auth/logout    - 登出(加入黑名单)        │ │
│  │  4. POST /auth/validate  - Token验证(Gateway调用)  │ │
│  │  5. GET  /auth/blacklist/check - 黑名单检查        │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │  权限管理API                                        │ │
│  │  - 角色CRUD                                         │ │
│  │  - 权限分配                                         │ │
│  │  - 权限变更 → 发送事件 → Gateway刷新缓存           │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 JWT Token设计

### Token结构

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "jti": "unique-token-id",
    "sub": "user-id",
    "userDomain": "MEMBER",
    "username": "john_doe",
    "authorities": [
      "user:read",
      "user:write"
    ],
    "iat": 1699516800,
    "exp": 1699520400
  },
  "signature": "..."
}
```

### 密钥管理

**推荐方案**: 使用**非对称加密 (RSA)**

```yaml
# Auth-Service配置
pot:
  jwt:
    private-key: classpath:keys/jwt_private_key.pem  # 用于签名
    public-key: classpath:keys/jwt_public_key.pem    # 用于验证
    access-token-ttl: 3600          # 1小时
    refresh-token-ttl: 2592000      # 30天

# Gateway配置
pot:
  jwt:
    public-key: classpath:keys/jwt_public_key.pem    # 只需公钥验证
```

**优势**:

- ✅ Gateway只需公钥，无法签发Token
- ✅ Auth-Service独占私钥，安全性高
- ✅ 支持分布式部署

---

## 🚀 Gateway实现示例

### 1. JWT验证Filter

```java

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;
    private final TokenBlacklistCache blacklistCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        // 1. 提取Token
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 本地验证Token (签名、过期时间)
            Claims claims = jwtTokenValidator.validate(token);

            // 3. 黑名单检查 (Redis本地缓存，100ms TTL)
            String jti = claims.get("jti", String.class);
            if (blacklistCache.isBlacklisted(jti)) {
                throw new TokenRevokedException("Token已撤销");
            }

            // 4. 构建Spring Security上下文
            Authentication auth = buildAuthentication(claims);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 5. 添加用户上下文Header (传递给下游服务)
            request.setAttribute("X-User-Id", claims.getSubject());
            request.setAttribute("X-User-Domain", claims.get("userDomain"));
            request.setAttribute("X-Authorities", claims.get("authorities"));

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\":\"Invalid token\"}");
        }
    }

    private Authentication buildAuthentication(Claims claims) {
        String userId = claims.getSubject();
        String userDomain = claims.get("userDomain", String.class);
        List<String> authorities = claims.get("authorities", List.class);

        Collection<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserPrincipal principal = new GatewayUserPrincipal(userId, userDomain);
        return new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
    }
}
```

### 2. Token黑名单缓存 (Gateway侧)

```java

@Component
public class TokenBlacklistCache {

    private final StringRedisTemplate redisTemplate;
    private final LoadingCache<String, Boolean> localCache;

    public TokenBlacklistCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 本地缓存，减少Redis调用
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))  // 10秒本地缓存
                .maximumSize(10000)
                .build(this::checkRedis);
    }

    public boolean isBlacklisted(String jti) {
        return localCache.get(jti);
    }

    private Boolean checkRedis(String jti) {
        String key = "auth:token:blacklist:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 监听Auth-Service发送的黑名单变更事件
    @EventListener
    public void onTokenRevoked(TokenRevokedEvent event) {
        localCache.invalidate(event.getJti());
    }
}
```

### 3. 限流Filter

```java

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String userId = (String) request.getAttribute("X-User-Id");
        String limitKey = "rate_limit:user:" + userId;

        // 令牌桶算法 (Redis + Lua)
        if (!rateLimiter.tryAcquire(limitKey, 100, 1, TimeUnit.SECONDS)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 🔄 Gateway如何调用Auth-Service

### 场景1: 黑名单同步 (推荐使用消息队列)

**方案A: Redis Pub/Sub**

```java
// Auth-Service: 发布黑名单事件
@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;

    public void revokeToken(String jti, Long expiresAt) {
        // 1. 写入Redis黑名单
        String key = "auth:token:blacklist:" + jti;
        long ttl = expiresAt - System.currentTimeMillis() / 1000;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);

        // 2. 发布消息通知Gateway
        redisTemplate.convertAndSend("auth:token:revoked", jti);
    }
}

// Gateway: 订阅消息
@Component
public class TokenRevokedListener implements MessageListener {
    private final TokenBlacklistCache blacklistCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String jti = new String(message.getBody());
        blacklistCache.invalidate(jti);  // 清除本地缓存
    }
}
```

**方案B: Feign调用 (不推荐，性能差)**

```java
// Gateway Feign Client
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthServiceClient {
    @GetMapping("/blacklist/check")
    boolean isBlacklisted(@RequestParam String jti);
}
```

### 场景2: 权限缓存刷新

**方案: Redis Pub/Sub + 本地缓存**

```java
// Auth-Service: 权限变更后发布事件
@EventListener
public void onRolePermissionChanged(RolePermissionGrantedEvent event) {
    // 发布消息
    redisTemplate.convertAndSend("auth:permission:changed", event.getRoleId());
}

// Gateway: 清除权限缓存
@Component
public class PermissionCacheInvalidator implements MessageListener {
    private final PermissionCache permissionCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String roleId = new String(message.getBody());
        permissionCache.evictByRole(roleId);
    }
}
```

---

## 📊 性能优化策略

### 1. 多级缓存

```
请求 → Gateway本地缓存 (Caffeine, 10s TTL)
         ↓ Miss
      → Redis缓存 (60s TTL)
         ↓ Miss
      → Auth-Service
```

### 2. 缓存Key设计

```
# 黑名单
auth:token:blacklist:{jti}       TTL = Token剩余有效期

# 用户权限
auth:permission:user:{userId}    TTL = 60s

# 角色权限
auth:permission:role:{roleId}    TTL = 300s
```

### 3. 性能目标

| 场景             | 目标延迟   | 缓存命中率       |
|----------------|--------|-------------|
| Gateway JWT验证  | <5ms   | N/A (本地计算)  |
| Gateway黑名单检查   | <2ms   | >99% (本地缓存) |
| Gateway权限检查    | <10ms  | >95%        |
| Auth-Service登录 | <200ms | N/A         |

---

## 🔧 配置示例

### Gateway配置

```yaml
spring:
  application:
    name: gateway
  cloud:
    gateway:
      routes:
        - id: auth-route
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=0

        - id: member-route
          uri: lb://member-service
          predicates:
            - Path=/api/member/**
          filters:
            - JwtAuthenticationFilter  # 自定义认证Filter
            - RateLimitFilter           # 限流Filter

pot:
  jwt:
    public-key: ${JWT_PUBLIC_KEY:classpath:keys/jwt_public_key.pem}

  security:
    whitelist: # 白名单，不需要认证
      - /auth/login
      - /auth/register
      - /auth/oauth2/callback/**
      - /actuator/health

  rate-limit:
    default-limit: 100          # 默认限流：100次/秒
    user-limit: 1000           # 认证用户：1000次/秒
```

### Auth-Service配置

```yaml
pot:
  jwt:
    private-key: ${JWT_PRIVATE_KEY:classpath:keys/jwt_private_key.pem}
    public-key: ${JWT_PUBLIC_KEY:classpath:keys/jwt_public_key.pem}
    access-token-ttl: 3600          # 1小时
    refresh-token-ttl: 2592000      # 30天
    issuer: pot-auth

  security:
    password-policy:
      min-length: 8
      require-uppercase: true
      require-number: true
      require-special-char: true
      max-history: 5                # 密码历史记录数

    login-attempt:
      max-attempts: 5
      lock-duration: 1800           # 锁定30分钟
```

---

## 🚨 安全考虑

### 1. Token泄露应对

**场景**: 用户怀疑Token泄露

**解决方案**:

```java
// 提供API撤销所有Token
@PostMapping("/auth/logout-all")
public void logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
    // 1. 查询该用户所有未过期的RefreshToken
    List<String> refreshTokens = refreshTokenRepository.findByPrincipal(principal);

    // 2. 全部加入黑名单
    refreshTokens.forEach(jti -> tokenBlacklistService.revokeToken(jti));

    // 3. 发布事件
    eventPublisher.publish(new AllTokensRevokedEvent(principal));
}
```

### 2. 防重放攻击

**方案**: 使用`jti` (JWT ID) + Redis去重

```java
// 敏感操作接口
@PostMapping("/transfer")
public void transfer(@RequestHeader("Authorization") String token) {
    String jti = jwtParser.parseJti(token);

    // 检查jti是否已使用
    String key = "auth:jti:used:" + jti;
    Boolean isUsed = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);

    if (Boolean.FALSE.equals(isUsed)) {
        throw new ReplayAttackException("请求重放");
    }

    // 执行业务逻辑
}
```

### 3. HTTPS强制

```yaml
server:
  ssl:
    enabled: true
  http2:
    enabled: true
```

---

## 📈 监控指标

### Gateway指标

```java

@Component
public class GatewayMetrics {
    private final MeterRegistry registry;

    // JWT验证成功率
    Counter.builder("gateway.jwt.validation.success").

    register(registry);
    Counter.builder("gateway.jwt.validation.failed").

    register(registry);

    // 黑名单检查耗时
    Timer.builder("gateway.blacklist.check.duration").

    register(registry);

    // 限流拦截数
    Counter.builder("gateway.ratelimit.rejected").

    register(registry);
}
```

### Auth-Service指标

```java
// 登录成功率
Counter.builder("auth.login.success").

tag("method",loginMethod).

register(registry);
Counter.

builder("auth.login.failed").

tag("reason",failReason).

register(registry);

// Token签发数
Counter.

builder("auth.token.issued").

tag("type",tokenType).

register(registry);

// 黑名单大小
Gauge.

builder("auth.blacklist.size",blacklistRepository::count).

register(registry);
```

---

## ✅ 最佳实践总结

1. ✅ **Gateway不调用Auth-Service验证Token** - 使用本地JWT验证
2. ✅ **黑名单使用Redis Pub/Sub同步** - 而非Feign调用
3. ✅ **多级缓存减少Redis压力** - Caffeine本地缓存 + Redis
4. ✅ **非对称加密保护私钥** - Gateway只持有公钥
5. ✅ **权限缓存自动失效** - 监听权限变更事件
6. ✅ **限流在Gateway层** - 保护后端服务
7. ✅ **监控覆盖关键路径** - 及时发现性能问题

---

**文档版本**: v1.0  
**最后更新**: 2025-11-09

