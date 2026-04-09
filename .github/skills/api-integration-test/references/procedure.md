# API 集成测试：完整执行流程

## 全局变量（执行过程中填充）

在开始前，声明以下变量并在步骤执行中逐一填充：

```
AUTH_BASE    = http://localhost:8081
MEMBER_BASE  = http://localhost:11000
TS           = <当前 Unix 时间戳后 6 位，如 853421>
NICKNAME     = test_<TS>
EMAIL        = test_<TS>@zing.test
PASSWORD     = Test@12345678
USER_DOMAIN  = USER
MEMBER_ID    = <注册/登录后获取>
ACCESS_TOKEN = <登录后获取>
REFRESH_TOKEN = <登录后获取>
TOKEN_ID     = <从 JWT payload 解析 jti 字段>
```

---

## Step 1 — Pre-flight 检查

### 1a. 检查服务健康状态

```bash
curl -s -o /dev/null -w "%{http_code}" ${AUTH_BASE}/actuator/health
curl -s -o /dev/null -w "%{http_code}" ${MEMBER_BASE}/actuator/health
```

期望：两个服务均返回 `200`。若服务未启动，终止测试并在报告中记录 `[BLOCKED - service not running]`。

### 1b. 检查 MCP 工具可用性

依次使用 `tool_search_tool_regex` 搜索以下 pattern，记录可用状态：

| MCP          | 搜索 pattern                             | 变量                |
| ------------ | ---------------------------------------- | ------------------- |
| mysql-mcp    | `mysql\|sql.*query\|execute.*sql`        | `$MYSQL_AVAILABLE`  |
| redis-mcp    | `redis\|cache.*get\|redis.*key`          | `$REDIS_AVAILABLE`  |
| rabbitmq-mcp | `rabbit\|rabbitmq\|amqp\|message.*queue` | `$RABBIT_AVAILABLE` |

若搜索返回空结果，对应变量设为 `false`，该层所有验证标记 `[SKIP - MCP not available]`。

---

## Step 2 — OpenAPI 发现

获取两个服务的接口列表，与 [endpoint-test-data.md](./endpoint-test-data.md) 中的覆盖列表对比：

```bash
# auth-service OpenAPI spec
curl -s ${AUTH_BASE}/v3/api-docs | python3 -c "
import sys, json
spec = json.load(sys.stdin)
for path, methods in spec.get('paths', {}).items():
    for method in methods:
        print(f'{method.upper()} {path}')
"

# member-service OpenAPI spec
curl -s ${MEMBER_BASE}/v3/api-docs | python3 -c "
import sys, json
spec = json.load(sys.stdin)
for path, methods in spec.get('paths', {}).items():
    for method in methods:
        print(f'{method.upper()} {path}')
"
```

记录发现的接口列表。若 `/v3/api-docs` 返回 404，使用 `/v3/api-docs/public` 重试。

---

## Step 3 — 测试数据生成

生成带时间戳的唯一测试用户：

```bash
TS=$(date +%s | tail -c 7)
NICKNAME="test_${TS}"
EMAIL="test_${TS}@zing.test"
PASSWORD="Test@12345678"
USER_DOMAIN="USER"
echo "Test user: nickname=${NICKNAME}, email=${EMAIL}"
```

---

## Step 4 — 注册测试

### 4a. 执行注册请求

```bash
REGISTER_RESP=$(curl -s -X POST ${AUTH_BASE}/api/v1/register \
  -H "Content-Type: application/json" \
  -d "{
    \"registerType\": \"USERNAME_PASSWORD\",
    \"nickname\": \"${NICKNAME}\",
    \"password\": \"${PASSWORD}\",
    \"userDomain\": \"${USER_DOMAIN}\"
  }")
echo ${REGISTER_RESP} | python3 -m json.tool
```

### 4b. 提取关键字段

```bash
MEMBER_ID=$(echo ${REGISTER_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['userId'])")
ACCESS_TOKEN=$(echo ${REGISTER_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])")
REFRESH_TOKEN=$(echo ${REGISTER_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['refreshToken'])")
```

### 4c. DB 验证（若 `$MYSQL_AVAILABLE=true`）

使用 mysql-mcp 执行查询。查询语句见 [verification-queries.md](./verification-queries.md#register)。

期望：`member_member` 表中存在 `nickname = '${NICKNAME}'` 且 `status = 'ACTIVE'` 的行。

**断言失败处理**：记录 `[FAIL]`，继续执行后续步骤，在报告末尾汇总。

---

## Step 5 — 登录 Bootstrap

> 注意：auth-service 对 `/api/v1/login` 有 5 req/IP/min 限流，两次登录尝试之间等待 1 秒。

### 5a. 执行登录请求

```bash
sleep 1
LOGIN_RESP=$(curl -s -X POST ${AUTH_BASE}/api/v1/login \
  -H "Content-Type: application/json" \
  -d "{
    \"loginType\": \"USERNAME_PASSWORD\",
    \"nickname\": \"${NICKNAME}\",
    \"password\": \"${PASSWORD}\",
    \"userDomain\": \"${USER_DOMAIN}\"
  }")
echo ${LOGIN_RESP} | python3 -m json.tool
```

> 若注册时已成功获取 `ACCESS_TOKEN` 和 `REFRESH_TOKEN`，此步骤可跳过（直接使用注册返回的 token）。

### 5b. 提取字段（覆盖注册返回的值）

```bash
ACCESS_TOKEN=$(echo ${LOGIN_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])")
REFRESH_TOKEN=$(echo ${LOGIN_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['refreshToken'])")
MEMBER_ID=$(echo ${LOGIN_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['userId'])")
```

### 5c. 解析 JWT 中的 Token ID（jti）

```bash
# JWT payload 是 base64 编码，取中间段
TOKEN_PAYLOAD=$(echo ${ACCESS_TOKEN} | cut -d. -f2 | base64 --decode 2>/dev/null || \
  echo ${ACCESS_TOKEN} | cut -d. -f2 | python3 -c "
import sys, base64, json
p = sys.stdin.read().strip()
p += '=' * (-len(p) % 4)
print(json.dumps(json.loads(base64.b64decode(p)), indent=2))
")
echo ${TOKEN_PAYLOAD}
TOKEN_ID=$(echo ${TOKEN_PAYLOAD} | python3 -c "import sys,json; print(json.load(sys.stdin).get('jti',''))")
```

### 5d. Redis 验证（若 `$REDIS_AVAILABLE=true`）

使用 redis-mcp 验证。命令见 [verification-queries.md](./verification-queries.md#login)。

期望：`auth:blacklist:{TOKEN_ID}` key **不存在**（token 尚未被吊销）。

---

## Step 6 — Token 刷新测试

### 6a. 执行刷新请求

```bash
REFRESH_RESP=$(curl -s -X POST ${AUTH_BASE}/api/v1/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"${REFRESH_TOKEN}\"}")
echo ${REFRESH_RESP} | python3 -m json.tool
```

### 6b. 更新 Token

```bash
ACCESS_TOKEN=$(echo ${REFRESH_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])")
REFRESH_TOKEN=$(echo ${REFRESH_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['refreshToken'])")
```

期望：响应 code 为 `0`，返回新的 `accessToken`。

---

## Step 7 — 登出测试

### 7a. 执行登出请求

```bash
LOGOUT_RESP=$(curl -s -X POST ${AUTH_BASE}/api/v1/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "{\"refreshToken\": \"${REFRESH_TOKEN}\"}")
echo ${LOGOUT_RESP} | python3 -m json.tool
```

期望：响应 code 为 `0`。

### 7b. Redis 黑名单验证（若 `$REDIS_AVAILABLE=true`）

命令见 [verification-queries.md](./verification-queries.md#logout)。

期望：`auth:blacklist:{TOKEN_ID}` key **存在**（token 已被吊销）。

### 7c. 重新登录（为后续 Member 测试获取新 token）

```bash
sleep 1
RE_LOGIN_RESP=$(curl -s -X POST ${AUTH_BASE}/api/v1/login \
  -H "Content-Type: application/json" \
  -d "{
    \"loginType\": \"USERNAME_PASSWORD\",
    \"nickname\": \"${NICKNAME}\",
    \"password\": \"${PASSWORD}\",
    \"userDomain\": \"${USER_DOMAIN}\"
  }")
ACCESS_TOKEN=$(echo ${RE_LOGIN_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])")
REFRESH_TOKEN=$(echo ${RE_LOGIN_RESP} | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['refreshToken'])")
```

---

## Step 8 — Member 读接口测试

### 8a. GET /me — 当前会员

```bash
curl -s -X GET ${MEMBER_BASE}/api/members/me \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | python3 -m json.tool
```

### 8b. GET /{memberId} — 按 ID 查询

```bash
curl -s -X GET ${MEMBER_BASE}/api/members/${MEMBER_ID} \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | python3 -m json.tool
```

### 8c. GET /{memberId}/permissions — 权限列表

```bash
curl -s -X GET ${MEMBER_BASE}/api/members/${MEMBER_ID}/permissions \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | python3 -m json.tool
```

### 8d. Redis 缓存验证（若 `$REDIS_AVAILABLE=true`）

命令见 [verification-queries.md](./verification-queries.md#member-read)。

对读接口：验证响应返回正确数据，并检查权限版本缓存 key `auth:perm:version:{USER_DOMAIN}:{MEMBER_ID}` 是否存在。

---

## Step 9 — Member 写接口测试

### 9a. PUT /{memberId}/profile — 更新资料

```bash
PROFILE_RESP=$(curl -s -X PUT ${MEMBER_BASE}/api/members/${MEMBER_ID}/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "{
    \"nickname\": \"${NICKNAME}_updated\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\",
    \"gender\": 1,
    \"bio\": \"Integration test bio\",
    \"countryCode\": \"CN\",
    \"timezone\": \"Asia/Shanghai\",
    \"locale\": \"zh_CN\"
  }")
echo ${PROFILE_RESP} | python3 -m json.tool
```

DB 验证见 [verification-queries.md](./verification-queries.md#update-profile)。

### 9b. PUT /{memberId}/password — 修改密码

```bash
NEW_PASSWORD="Test@NewPass9876"
PASSWD_RESP=$(curl -s -X PUT ${MEMBER_BASE}/api/members/${MEMBER_ID}/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "{
    \"oldPassword\": \"${PASSWORD}\",
    \"newPassword\": \"${NEW_PASSWORD}\"
  }")
echo ${PASSWD_RESP} | python3 -m json.tool
PASSWORD=${NEW_PASSWORD}
```

DB 验证见 [verification-queries.md](./verification-queries.md#change-password)。

### 9c. POST /{memberId}/lock — 锁定账户

```bash
LOCK_RESP=$(curl -s -X POST ${MEMBER_BASE}/api/members/${MEMBER_ID}/lock \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")
echo ${LOCK_RESP} | python3 -m json.tool
```

DB 验证见 [verification-queries.md](./verification-queries.md#lock)。

### 9d. POST /{memberId}/unlock — 解锁账户

```bash
UNLOCK_RESP=$(curl -s -X POST ${MEMBER_BASE}/api/members/${MEMBER_ID}/unlock \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")
echo ${UNLOCK_RESP} | python3 -m json.tool
```

DB 验证见 [verification-queries.md](./verification-queries.md#unlock)。

### 9e. RabbitMQ 事件验证（若 `$RABBIT_AVAILABLE=true`）

对以上每个写操作，检查对应的 domain event 是否入队。命令见 [verification-queries.md](./verification-queries.md#rabbitmq)。

---

## Step 10 — 报告生成

将所有步骤的结果按 [report-template.md](../assets/report-template.md) 格式整理，写入：

```
docs/api-test-report-{YYYY-MM-DD}.md
```

报告内容包含：

- 测试运行时间和环境信息
- 摘要表格（接口 | HTTP 状态 | DB | Redis | MQ | 结论）
- 每个接口的详细块（curl 命令、原始响应、验证结果）
- 测试数据清理 SQL（MEMBER_ID、NICKNAME 的实际值）
- 已知限制和需人工验证的项目

写入报告后，在 chat 中输出摘要表格，告知用户报告路径。
