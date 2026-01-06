# 认证端点限流实现

## 概述

为 auth-service 的所有认证相关端点添加了基于 IP 的分布式限流，使用 framework-starter-ratelimit 实现，基于 Redis 存储限流状态。

## 已添加限流的端点

### 1. 登录端点

- **路径**: `POST /auth/api/v1/login`
- **限流策略**: IP 限流
- **速率限制**: 5 次/秒
- **错误消息**: "登录请求过于频繁，请稍后再试"
- **目的**: 防止暴力破解攻击

### 2. Token 刷新端点

- **路径**: `POST /auth/api/v1/refresh`
- **限流策略**: IP 限流
- **速率限制**: 10 次/秒
- **错误消息**: "Token 刷新请求过于频繁，请稍后再试"
- **目的**: 防止 Token 刷新滥用

### 3. 注册端点

- **路径**: `POST /auth/api/v1/register`
- **限流策略**: IP 限流
- **速率限制**: 3 次/秒
- **错误消息**: "注册请求过于频繁，请稍后再试"
- **目的**: 防止批量注册攻击

### 4. 一键认证端点

- **路径**: `POST /auth/api/v1/authenticate`
- **限流策略**: IP 限流
- **速率限制**: 5 次/秒
- **错误消息**: "认证请求过于频繁，请稍后再试"
- **目的**: 防止认证接口滥用

### 5. 邮件验证码发送

- **路径**: `POST /auth/code/email`
- **限流策略**: IP 限流
- **速率限制**: 1 次/秒
- **错误消息**: "验证码发送过于频繁，请稍后再试"
- **目的**: 防止验证码轰炸、邮件资源滥用

### 6. 短信验证码发送

- **路径**: `POST /auth/code/sms`
- **限流策略**: IP 限流
- **速率限制**: 1 次/秒
- **错误消息**: "验证码发送过于频繁，请稍后再试"
- **目的**: 防止短信轰炸、运营成本控制

## 技术实现

### 核心注解

```java
@RateLimit(
    type = RateLimitMethodEnum.IP_BASED,  // 基于IP限流
    rate = 5.0,                            // 每秒5次
    message = "请求过于频繁，请稍后再试"      // 自定义错误消息
)
```

### 限流类型说明

#### IP_BASED（基于 IP）

- **使用场景**: 所有认证端点
- **优点**:
  - 防止来自单一 IP 的暴力攻击
  - 不依赖用户登录状态
  - 适用于匿名访问的端点
- **限制 key**: `ratelimit:ip:{endpoint}:{ip}`

#### USER_BASED（基于用户）

- **使用场景**: 已登录用户的敏感操作
- **优点**:
  - 精确限制每个用户的请求频率
  - 防止账号被盗用后的批量操作
- **限制 key**: `ratelimit:user:{endpoint}:{userId}`
- **注**: 当前认证端点未使用，因为用户尚未登录

#### FIXED（固定限流）

- **使用场景**: 全局限流
- **优点**:
  - 保护系统整体资源
  - 防止系统过载
- **限制 key**: `ratelimit:fixed:{endpoint}`

## 配置文件

### application.yml

```yaml
pot:
  ratelimit:
    enabled: true # 启用限流
    type: redis # 使用Redis实现分布式限流
```

### Redis 依赖

限流功能依赖 Redis，需要确保 Redis 已正确配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
```

## 限流算法

framework-starter-ratelimit 使用**令牌桶算法**：

1. 每个限流 key 有一个令牌桶
2. 令牌以固定速率（rate）添加到桶中
3. 请求到来时尝试从桶中获取令牌
4. 如果桶中有令牌，请求通过，消耗一个令牌
5. 如果桶中无令牌，请求被拒绝

### 实现优势

- ✅ 支持突发流量（桶容量 = rate）
- ✅ 平滑限流，避免流量锯齿
- ✅ 基于 Redis 的分布式实现
- ✅ 支持多实例部署

## 错误响应

当请求被限流时，返回 HTTP 429 状态码：

```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "msg": "登录请求过于频繁，请稍后再试",
  "data": null
}
```

## 监控建议

### 1. Redis 监控

监控限流相关的 Redis key：

```bash
# 查看所有限流key
redis-cli KEYS "ratelimit:*"

# 查看特定端点的限流状态
redis-cli GET "ratelimit:ip:/auth/api/v1/login:192.168.1.100"
```

### 2. 日志监控

framework-starter-ratelimit 会在限流触发时记录日志：

```
[RateLimit] Request blocked: ip=192.168.1.100, endpoint=/auth/api/v1/login, rate=5.0
```

### 3. 指标监控

建议添加 Prometheus 指标：

- `auth_ratelimit_blocked_total`: 总被限流次数
- `auth_ratelimit_blocked_by_endpoint`: 按端点分组的限流次数
- `auth_ratelimit_blocked_by_ip`: 按 IP 分组的限流次数

## 调优建议

### 1. 根据实际流量调整速率

当前配置基于经验值，建议根据实际流量调整：

- **登录**: 正常用户不会每秒登录超过 1 次，5 次/秒已足够宽松
- **注册**: 3 次/秒足以应对正常注册流程（包括重试）
- **验证码**: 1 次/秒防止验证码轰炸，同时允许用户重新发送

### 2. 区分内外网流量

如果部署在反向代理后，需要正确获取真实 IP：

```java
// IpUtils.getClientIp() 已支持X-Forwarded-For等header
```

### 3. 白名单机制

对于可信 IP（如内部服务、监控系统），可以添加白名单：

```java
@RateLimit(
    type = RateLimitMethodEnum.IP_BASED,
    rate = 5.0,
    key = "#request.getRemoteAddr()"  // 可以通过SpEL自定义key
)
```

### 4. 多级限流

可以同时应用多个限流策略：

```java
@RateLimit(type = IP_BASED, rate = 10.0)     // 单IP限流
@RateLimit(type = FIXED, rate = 1000.0)      // 全局限流
public R<LoginResponse> login(...) { }
```

## 安全增强建议

### 1. 结合验证码

当检测到频繁失败时，要求用户输入验证码：

- 连续 3 次登录失败 → 显示图形验证码
- 连续 5 次登录失败 → 显示滑动验证码
- 连续 10 次登录失败 → 锁定账号 15 分钟

### 2. IP 黑名单

对于持续触发限流的 IP，自动加入黑名单：

```yaml
pot:
  security:
    ip-blacklist:
      enabled: true
      threshold: 100 # 触发限流100次后加入黑名单
      duration: 3600 # 黑名单持续时间（秒）
```

### 3. 账号锁定

对于暴力破解尝试，锁定被攻击的账号：

- 同一账号连续 5 次密码错误 → 锁定 15 分钟
- 同一账号 1 小时内 10 次密码错误 → 锁定 1 小时
- 同一账号 24 小时内 20 次密码错误 → 锁定 24 小时

## 测试验证

### 单元测试

```bash
# 测试限流功能
curl -X POST http://localhost:8081/auth/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":"USERNAME_PASSWORD","username":"test","password":"test123","userDomain":"MEMBER"}'

# 快速重复请求（应被限流）
for i in {1..10}; do
  curl -X POST http://localhost:8081/auth/api/v1/login \
    -H "Content-Type: application/json" \
    -d '{"loginType":"USERNAME_PASSWORD","username":"test","password":"test123","userDomain":"MEMBER"}'
done
```

### 压力测试

使用 Apache Bench 或 JMeter 进行压力测试：

```bash
ab -n 100 -c 10 -p login.json -T application/json \
  http://localhost:8081/auth/api/v1/login
```

## 限流规则总结

| 端点       | 路径                      | 速率限制 | 限流类型 | 说明            |
| ---------- | ------------------------- | -------- | -------- | --------------- |
| 登录       | /auth/api/v1/login        | 5/秒     | IP       | 防止暴力破解    |
| Token 刷新 | /auth/api/v1/refresh      | 10/秒    | IP       | 防止 Token 滥用 |
| 注册       | /auth/api/v1/register     | 3/秒     | IP       | 防止批量注册    |
| 一键认证   | /auth/api/v1/authenticate | 5/秒     | IP       | 防止接口滥用    |
| 邮件验证码 | /auth/code/email          | 1/秒     | IP       | 防止邮件轰炸    |
| 短信验证码 | /auth/code/sms            | 1/秒     | IP       | 防止短信轰炸    |

所有限流都基于 IP 地址，使用 Redis 存储限流状态，支持分布式部署。
