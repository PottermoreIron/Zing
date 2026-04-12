# Auth Endpoint Rate Limiting

## Overview

IP-based distributed rate limiting has been applied to all authentication-related endpoints in `auth-service`, implemented via `framework-starter-ratelimit` and backed by Redis.

## Rate-Limited Endpoints

### 1. Login

- **Path**: `POST /auth/api/v1/login`
- **Strategy**: IP-based
- **Rate**: 5 requests/second
- **Error message**: "Too many login attempts. Please try again later."
- **Purpose**: Prevent brute-force attacks

### 2. Token Refresh

- **Path**: `POST /auth/api/v1/refresh`
- **Strategy**: IP-based
- **Rate**: 10 requests/second
- **Error message**: "Too many token refresh requests. Please try again later."
- **Purpose**: Prevent token refresh abuse

### 3. Registration

- **Path**: `POST /auth/api/v1/register`
- **Strategy**: IP-based
- **Rate**: 3 requests/second
- **Error message**: "Too many registration requests. Please try again later."
- **Purpose**: Prevent bulk registration attacks

### 4. One-Stop Authentication

- **Path**: `POST /auth/api/v1/authenticate`
- **Strategy**: IP-based
- **Rate**: 5 requests/second
- **Error message**: "Too many authentication requests. Please try again later."
- **Purpose**: Prevent authentication endpoint abuse

### 5. Email Verification Code

- **Path**: `POST /auth/code/email`
- **Strategy**: IP-based
- **Rate**: 1 request/second
- **Error message**: "Verification code sent too frequently. Please try again later."
- **Purpose**: Prevent verification code bombing and email resource abuse

### 6. SMS Verification Code

- **Path**: `POST /auth/code/sms`
- **Strategy**: IP-based
- **Rate**: 1 request/second
- **Error message**: "Verification code sent too frequently. Please try again later."
- **Purpose**: Prevent SMS bombing and control messaging costs

## Technical Implementation

### Core Annotation

```java
@RateLimit(
    type = RateLimitMethodEnum.IP_BASED,  // IP-based rate limiting
    rate = 5.0,                            // 5 requests per second
    message = "Too many requests. Please try again later."
)
```

### Rate Limit Types

#### IP_BASED

- **Use case**: All authentication endpoints
- **Advantages**:
  - Guards against brute-force attacks from a single IP
  - Does not depend on user login state
  - Suitable for anonymous endpoints
- **Key pattern**: `ratelimit:ip:{endpoint}:{ip}`

#### USER_BASED

- **Use case**: Sensitive operations for authenticated users
- **Advantages**:
  - Precisely limits request frequency per user
  - Prevents bulk operations after account compromise
- **Key pattern**: `ratelimit:user:{endpoint}:{userId}`
- **Note**: Not used for auth endpoints because the user is not yet logged in

#### FIXED

- **Use case**: Global rate limiting
- **Advantages**:
  - Protects overall system resources
  - Prevents system overload
- **Key pattern**: `ratelimit:fixed:{endpoint}`

## Configuration

### application.yml

```yaml
pot:
  ratelimit:
    enabled: true  # Enable rate limiting
    type: redis    # Use Redis for distributed rate limiting
```

### Redis Dependency

Rate limiting depends on Redis. Ensure Redis is properly configured:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
```

## Rate Limiting Algorithm

`framework-starter-ratelimit` uses the **token bucket algorithm**:

1. Each rate limit key maintains a token bucket.
2. Tokens are added to the bucket at a fixed rate.
3. Incoming requests attempt to acquire a token from the bucket.
4. If a token is available, the request passes and one token is consumed.
5. If no token is available, the request is rejected.

### Implementation Advantages

- ✅ Supports burst traffic (bucket capacity = rate)
- ✅ Smooth limiting without traffic spikes
- ✅ Redis-backed distributed implementation
- ✅ Supports multi-instance deployment

## Error Response

When a request is rate-limited, HTTP 429 is returned:

```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "msg": "Too many login attempts. Please try again later.",
  "data": null
}
```

## Monitoring Recommendations

### 1. Redis Monitoring

Monitor rate limit-related Redis keys:

```bash
# List all rate limit keys
redis-cli KEYS "ratelimit:*"

# Inspect a specific endpoint's rate limit state
redis-cli GET "ratelimit:ip:/auth/api/v1/login:192.168.1.100"
```

### 2. Log Monitoring

`framework-starter-ratelimit` logs when rate limiting is triggered:

```
[RateLimit] Request blocked: ip=192.168.1.100, endpoint=/auth/api/v1/login, rate=5.0
```

### 3. Metrics Monitoring

Recommended Prometheus metrics:

- `auth_ratelimit_blocked_total`: total number of blocked requests
- `auth_ratelimit_blocked_by_endpoint`: blocked requests grouped by endpoint
- `auth_ratelimit_blocked_by_ip`: blocked requests grouped by IP

## Tuning Recommendations

### 1. Adjust Rates Based on Real Traffic

Current limits are based on empirical values; tune them against real traffic:

- **Login**: Normal users rarely log in more than once per second; 5/s is already permissive.
- **Registration**: 3/s covers normal registration flows including retries.
- **Verification code**: 1/s prevents bombing while still allowing resends.

### 2. Distinguish Internal vs. External Traffic

When deployed behind a reverse proxy, ensure the real client IP is extracted correctly:

```java
// IpUtils.getClientIp() already supports X-Forwarded-For and similar headers
```

### 3. Allowlist Mechanism

For trusted IPs (e.g., internal services, monitoring systems), add an allowlist:

```java
@RateLimit(
    type = RateLimitMethodEnum.IP_BASED,
    rate = 5.0,
    key = "#request.getRemoteAddr()"  // Custom key via SpEL
)
```

### 4. Multi-Level Rate Limiting

Multiple rate limit strategies can be applied simultaneously:

```java
@RateLimit(type = IP_BASED, rate = 10.0)      // Per-IP limit
@RateLimit(type = FIXED, rate = 1000.0)       // Global limit
public R<LoginResponse> login(...) { }
```

## Security Hardening Recommendations

### 1. Combine with CAPTCHA

Require CAPTCHA when frequent failures are detected:

- 3 consecutive login failures → display image CAPTCHA
- 5 consecutive failures → display slider CAPTCHA
- 10 consecutive failures → lock account for 15 minutes

### 2. IP Blocklist

Automatically blocklist IPs that repeatedly trigger rate limits:

```yaml
pot:
  security:
    ip-blacklist:
      enabled: true
      threshold: 100  # Add to blocklist after 100 triggered limits
      duration: 3600  # Blocklist duration in seconds
```

### 3. Account Lockout

Lock accounts under brute-force attempts:

- 5 consecutive wrong passwords for the same account → lock for 15 minutes
- 10 wrong passwords within 1 hour → lock for 1 hour
- 20 wrong passwords within 24 hours → lock for 24 hours

## Test Verification

### Manual Test

```bash
# Test rate limiting
curl -X POST http://localhost:8081/auth/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":"USERNAME_PASSWORD","username":"test","password":"test123","userDomain":"MEMBER"}'

# Rapid repeated requests (should be rate-limited)
for i in {1..10}; do
  curl -X POST http://localhost:8081/auth/api/v1/login \
    -H "Content-Type: application/json" \
    -d '{"loginType":"USERNAME_PASSWORD","username":"test","password":"test123","userDomain":"MEMBER"}'
done
```

### Load Test

Use Apache Bench or JMeter:

```bash
ab -n 100 -c 10 -p login.json -T application/json \
  http://localhost:8081/auth/api/v1/login
```

## Rate Limit Rule Summary

| Endpoint              | Path                       | Rate   | Type | Purpose                  |
| --------------------- | -------------------------- | ------ | ---- | ------------------------ |
| Login                 | /auth/api/v1/login         | 5/sec  | IP   | Prevent brute-force      |
| Token refresh         | /auth/api/v1/refresh       | 10/sec | IP   | Prevent token abuse      |
| Registration          | /auth/api/v1/register      | 3/sec  | IP   | Prevent bulk registration |
| One-stop auth         | /auth/api/v1/authenticate  | 5/sec  | IP   | Prevent endpoint abuse   |
| Email verification    | /auth/code/email           | 1/sec  | IP   | Prevent email bombing    |
| SMS verification      | /auth/code/sms             | 1/sec  | IP   | Prevent SMS bombing      |

All rate limits are IP-based, backed by Redis, and work across distributed deployments.
