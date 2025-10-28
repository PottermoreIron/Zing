# Spring Security 快速开始指南

## 🚀 快速开始

### 1. 编译项目

```bash
cd /Users/yecao/Project/Pot/Zing/zing

# 清理并安装（跳过测试）
mvn clean install -DskipTests

# 或者只编译framework-starter-security模块
cd framework/framework-starter-security
mvn clean install -DskipTests
```

### 2. 启动服务

#### 2.1 启动Member Service

```bash
cd member/member-service
mvn spring-boot:run
```

#### 2.2 启动Auth Service

```bash
cd auth/auth-service
mvn spring-boot:run
```

### 3. 测试API

#### 3.1 用户注册

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "password": "Password123",
    "confirmPassword": "Password123"
  }'
```

#### 3.2 用户登录

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "Password123"
  }'
```

**响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEyMzQ1Niw...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEyMzQ1Niw...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 123456,
    "username": "测试用户",
    "nickname": "测试用户"
  }
}
```

#### 3.3 访问受保护的API

```bash
# 获取当前用户信息
curl -X GET http://localhost:8082/api/members/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### 3.4 刷新Token

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

#### 3.5 用户登出

```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 📋 权限控制示例

### 1. 使用@PreAuthorize注解

```java

@RestController
@RequestMapping("/api/users")
public class UserController {

    // 需要特定权限
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public R<UserDTO> getUser(@PathVariable Long id) {
        // ...
    }

    // 需要特定角色
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteUser(@PathVariable Long id) {
        // ...
    }

    // 复合条件：需要ADMIN角色且有删除权限
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:delete')")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        // ...
    }

    // 任意条件满足：ADMIN角色或者是用户本人
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        // ...
    }
}
```

### 2. 使用自定义注解

```java

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    @RequiresPermission(value = {"product:read", "product:list"}, logical = Logical.OR)
    public R<List<Product>> listProducts() {
        // ...
    }

    @PostMapping
    @RequiresRole(value = {"ADMIN", "PRODUCT_MANAGER"}, logical = Logical.OR)
    public R<Product> createProduct(@RequestBody Product product) {
        // ...
    }
}
```

### 3. 防重复提交

```java

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    @PreventResubmit(interval = 5, message = "订单提交过于频繁，请稍后再试")
    public R<Order> createOrder(@RequestBody Order order) {
        // 5秒内不能重复提交
        // ...
    }
}
```

### 4. 编程式权限检查

```java

@Service
public class UserService {

    public void updateUser(Long userId, UserDTO dto) {
        // 获取当前用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 检查是否是用户本人或管理员
        if (!userId.equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException("无权修改他人信息");
        }

        // 执行业务逻辑
        // ...
    }
}
```

---

## 🔧 常见问题

### Q1: 编译错误 - Cannot resolve symbol 'security'

**解决方案：**

```bash
# 先编译framework-starter-security模块
cd framework/framework-starter-security
mvn clean install -DskipTests

# 再编译依赖它的模块
cd ../../auth/auth-service
mvn clean install -DskipTests
```

### Q2: Member表没有password字段

**解决方案：**

```sql
ALTER TABLE member
    ADD COLUMN password VARCHAR(255) COMMENT '密码（BCrypt加密）';
```

### Q3: Redis连接失败

**检查Redis是否运行：**

```bash
redis-cli ping
# 应返回: PONG
```

**启动Redis：**

```bash
# macOS (使用Homebrew)
brew services start redis

# 或直接运行
redis-server
```

### Q4: JWT密钥警告

**生产环境配置：**

```yaml
# application-prod.yml
zing:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY}
```

```bash
# 设置环境变量
export JWT_SECRET_KEY="your-256-bit-secret-key-here-please-change-this"
```

### Q5: Token过期时间配置

```yaml
zing:
  security:
    jwt:
      # AccessToken有效期：1小时 = 3600000毫秒
      access-token-validity: 3600000

      # RefreshToken有效期：30天 = 2592000000毫秒
      refresh-token-validity: 2592000000
```

---

## 📱 Postman测试集合

### 环境变量设置

```json
{
  "auth_service_url": "http://localhost:8081",
  "member_service_url": "http://localhost:8082",
  "access_token": "",
  "refresh_token": ""
}
```

### 测试流程

1. 注册用户
2. 登录获取Token（自动保存到环境变量）
3. 使用Token访问受保护的API
4. Token过期后使用RefreshToken刷新
5. 登出

---

## 🎓 学习资源

### 核心类说明

| 类名                         | 作用                  | 位置                         |
|----------------------------|---------------------|----------------------------|
| `SecurityUser`             | Spring Security用户实体 | framework-starter-security |
| `JwtTokenProvider`         | JWT Token生成与解析      | framework-starter-security |
| `JwtTokenStore`            | Token存储（Redis）      | framework-starter-security |
| `SecurityUtils`            | Security工具类         | framework-starter-security |
| `AuthenticationService`    | 认证服务                | auth-service               |
| `MemberUserDetailsService` | 用户详情服务              | auth-service               |

### 关键注解

| 注解                    | 说明      | 示例                                                                         |
|-----------------------|---------|----------------------------------------------------------------------------|
| `@PreAuthorize`       | 方法执行前鉴权 | `@PreAuthorize("hasRole('ADMIN')")`                                        |
| `@PostAuthorize`      | 方法执行后鉴权 | `@PostAuthorize("returnObject.userId == authentication.principal.userId")` |
| `@Secured`            | 基于角色的鉴权 | `@Secured("ROLE_ADMIN")`                                                   |
| `@RequiresPermission` | 自定义权限注解 | `@RequiresPermission("user:delete")`                                       |
| `@PreventResubmit`    | 防重复提交   | `@PreventResubmit(interval = 5)`                                           |

---

## ✅ 验证清单

- [ ] framework-starter-security模块编译成功
- [ ] auth-service启动成功
- [ ] member-service启动成功
- [ ] Redis服务运行正常
- [ ] 用户注册接口测试通过
- [ ] 用户登录接口测试通过
- [ ] Token刷新接口测试通过
- [ ] 受保护API访问测试通过
- [ ] 权限拦截测试通过
- [ ] 防重复提交测试通过

---

**Happy Coding! 🎉**

# Spring Security 集成完成报告

## 📋 项目概览

本次已成功将工业级Spring Security安全框架集成到Zing微服务项目中，采用**JWT无状态认证**方案，实现了专业、可扩展的安全架构。

---

## ✅ 已完成的工作

### 1. **Framework层 - 核心Security模块** ✓

创建了 `framework-starter-security` 模块，包含以下核心组件：

#### 1.1 配置类

- ✅ `SecurityProperties` - Security配置属性
- ✅ `SecurityAutoConfiguration` - 自动配置类
- ✅ `PermissionEvaluatorImpl` - 权限评估器

#### 1.2 JWT核心组件

- ✅ `JwtTokenProvider` - JWT Token生成与解析
- ✅ `JwtTokenStore` - Token存储（Redis）
- ✅ `JwtAuthenticationFilter` - JWT认证过滤器

#### 1.3 Security用户模型

- ✅ `SecurityUser` - Spring Security用户实体
- ✅ `SecurityUtils` - Security工具类

#### 1.4 异常处理器

- ✅ `AuthenticationEntryPointImpl` - 认证入口（401处理）
- ✅ `AccessDeniedHandlerImpl` - 访问拒绝处理器（403处理）
- ✅ `LogoutSuccessHandlerImpl` - 登出成功处理器

#### 1.5 注解支持

- ✅ `@RequiresPermission` - 权限注解
- ✅ `@RequiresRole` - 角色注解
- ✅ `@PreventResubmit` - 防重复提交注解
- ✅ `PreventResubmitAspect` - 防重复提交切面

---

### 2. **Auth Service - 认证服务改造** ✓

#### 2.1 Security配置

- ✅ `AuthSecurityConfig` - 认证服务Security配置
- ✅ `application-security.yml` - Security配置文件

#### 2.2 用户认证

- ✅ `MemberUserDetailsService` - 会员用户详情服务
- ✅ `AuthenticationService` - 认证业务服务（登录/注册/刷新Token/登出）

#### 2.3 控制器与DTO

- ✅ `AuthController` - 认证控制器
- ✅ `LoginRequest` - 登录请求
- ✅ `RegisterRequest` - 注册请求
- ✅ `RefreshTokenRequest` - 刷新Token请求
- ✅ `TokenResponse` - Token响应

---

### 3. **Member Service - 业务服务安全集成** ✓

#### 3.1 Security配置

- ✅ `MemberSecurityConfig` - 会员服务Security配置
- ✅ `MemberController` - 示例控制器（展示权限注解使用）

---

## 🏗️ 架构设计亮点

### 1. **分层清晰**

```
Gateway (未实现) → Auth Service → Business Services
     ↓                  ↓                ↓
  路由鉴权          认证管理         方法鉴权
```

### 2. **JWT双Token机制**

- **AccessToken**: 短期有效（1小时），用于API访问
- **RefreshToken**: 长期有效（30天），用于刷新AccessToken

### 3. **Redis存储策略**

- Token黑名单（用于登出和强制下线）
- RefreshToken存储
- 在线用户管理
- 防重复提交缓存

### 4. **灵活的权限模型**

支持多种权限验证方式：

```java
// 1. 角色验证
@PreAuthorize("hasRole('ADMIN')")

// 2. 权限验证
@PreAuthorize("hasAuthority('user:delete')")

// 3. 复合条件
@PreAuthorize("hasRole('ADMIN') and hasAuthority('user:delete')")

// 4. 自定义注解
@RequiresPermission({"user:read", "user:update"})
@RequiresRole(value = "ADMIN", logical = Logical.OR)
```

---

## 📖 使用指南

### 1. **用户注册**

```http
POST /auth/register
Content-Type: application/json

{
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "password": "Password123",
  "confirmPassword": "Password123"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 123456,
    "username": "张三",
    "nickname": "张三"
  }
}
```

### 2. **用户登录**

```http
POST /auth/login
Content-Type: application/json

{
  "username": "zhangsan@example.com",
  "password": "Password123"
}
```

### 3. **刷新Token**

```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 4. **用户登出**

```http
POST /auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 5. **访问受保护的API**

```http
GET /api/members/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 💻 代码示例

### 1. **在Controller中使用权限控制**

```java

@RestController
@RequestMapping("/api/users")
public class UserController {

    // 任何认证用户都可以访问
    @GetMapping("/me")
    public R<UserDTO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        // ...
    }

    // 需要user:update权限
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        // ...
    }

    // 需要ADMIN角色
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteUser(@PathVariable Long id) {
        // ...
    }

    // 防重复提交
    @PostMapping("/action")
    @PreventResubmit(interval = 5, message = "操作过于频繁")
    public R<Void> doSomething() {
        // ...
    }
}
```

### 2. **获取当前用户信息**

```java
// 获取当前用户ID
Long userId = SecurityUtils.getCurrentUserId();

// 获取当前用户名
String username = SecurityUtils.getCurrentUsername();

// 获取当前用户对象
Optional<SecurityUser> user = SecurityUtils.getCurrentUser();

// 获取用户角色
Set<String> roles = SecurityUtils.getCurrentUserRoles();

// 获取用户权限
Set<String> permissions = SecurityUtils.getCurrentUserPermissions();

// 检查权限
boolean hasPermission = SecurityUtils.hasPermission("user:delete");
boolean hasRole = SecurityUtils.hasRole("ADMIN");
```

### 3. **密码加密与验证**

```java
// 加密密码
String encodedPassword = SecurityUtils.encodePassword("rawPassword");

// 验证密码
boolean matches = SecurityUtils.matchesPassword("rawPassword", encodedPassword);

// 检查密码强度
boolean isStrong = SecurityUtils.isStrongPassword("Password123");
```

---

## ⚙️ 配置说明

### application.yml 配置

```yaml
zing:
  security:
    enabled: true                    # 是否启用Security
    csrf-enabled: false              # 是否启用CSRF（JWT模式建议关闭）
    session-strategy: STATELESS      # 会话策略：STATELESS/STATEFUL

    jwt:
      secret-key: ${JWT_SECRET_KEY}  # JWT密钥（生产环境使用环境变量）
      access-token-validity: 3600000   # AccessToken有效期（毫秒）
      refresh-token-validity: 2592000000 # RefreshToken有效期（毫秒）
      header: Authorization          # Token请求头名称
      prefix: "Bearer "              # Token前缀
      issuer: zing                   # Token签发者

    whitelist: # 白名单（无需认证）
      - /auth/**
      - /oauth2/**
      - /actuator/health
      - /swagger-ui/**

    cache:
      enabled: true                  # 是否启用权限缓存
      ttl: 1800                      # 缓存过期时间（秒）
```

---

## 🔒 安全特性

### 1. **密码安全**

- ✅ BCrypt加密算法
- ✅ 密码强度验证
- ✅ 密码不存储在Token中

### 2. **Token安全**

- ✅ HS256签名算法
- ✅ Token过期验证
- ✅ Token类型验证（ACCESS/REFRESH）
- ✅ Token黑名单机制

### 3. **会话管理**

- ✅ 无状态JWT认证
- ✅ RefreshToken轮换
- ✅ 在线用户管理
- ✅ 强制下线功能

### 4. **防护机制**

- ✅ 防重复提交
- ✅ 防暴力破解（可配合限流）
- ✅ 统一异常处理
- ✅ 详细的操作���志

---

## 🚀 下一步工作建议

### 阶段一：权限管理（优先级：⭐⭐⭐⭐⭐）

1. 创建权限表结构（sys_role, sys_permission, sys_member_role, sys_role_permission）
2. 实现角色管理服务
3. 实现权限管理服务
4. 从数据库动态加载用户权限

### 阶段二：Gateway集成（优先级：⭐⭐⭐⭐）

1. 在Gateway添加JWT验证Filter
2. 实现路由级别鉴权
3. 实现服务间认证
4. 配置全局异常处理

### 阶段三：OAuth2增强（优先级：⭐⭐⭐）

1. 整合现有OAuth2登录流程
2. 统一Token生成逻辑
3. 支持社交账号绑定/解绑

### 阶段四：高级特性（优先级：⭐⭐）

1. 数据权限过滤（行级权限）
2. 多租户支持
3. 操作审计日志
4. 登录设备管理
5. 异地登录检测

### 阶段五：监控与优化（优先级：⭐）

1. 安全事件监控
2. 性能优化（权限缓存）
3. 压力测试
4. 安全加固

---

## 📝 注意事项

### 1. **生产环境配置**

⚠️ **必须修改JWT密钥**

```bash
# 使用环境变量设置
export JWT_SECRET_KEY="your-very-long-and-secure-secret-key-at-least-256-bits"
```

### 2. **Member��需要添加password字段**

```sql
ALTER TABLE member
    ADD COLUMN password VARCHAR(255) COMMENT '密码（BCrypt加密）';
```

### 3. **Maven依赖刷新**

执行以下命令刷新依赖：

```bash
cd /Users/yecao/Project/Pot/Zing/zing
mvn clean install -DskipTests
```

### 4. **Redis必须运行**

确保Redis服务正常运行，用于存储Token和防重复提交缓存。

---

## 🎯 核心优势

1. **工业级标准** - 遵循Spring Security最佳实践
2. **高度解耦** - 通过Starter实现安全能力复用
3. **易于扩展** - 支持自定义认证Provider和权限评估器
4. **性能优化** - JWT无状态 + Redis缓存
5. **完善文档** - 详细的代码注释和使用示例

---

## 📚 相关文档

- [Spring Security官方文档](https://docs.spring.io/spring-security/reference/)
- [JWT最佳实践](https://tools.ietf.org/html/rfc8725)
- [OWASP安全指南](https://owasp.org/www-project-top-ten/)

---

**作者**: Pot  
**日期**: 2025-01-24  
**版本**: v1.0.0

