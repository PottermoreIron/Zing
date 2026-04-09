# 数据层验证查询参考

每个操作完成后，使用对应的 MCP 工具执行以下查询验证数据状态。

占位符：

- `{MEMBER_ID}` — 测试用户的 memberId（Long）
- `{NICKNAME}` — 测试用户昵称（含 `test_` 前缀）
- `{TOKEN_ID}` — 从 JWT payload 中解析的 `jti` 字段
- `{USER_DOMAIN}` — 用户域，值为 `USER`

---

## MySQL 验证

数据库名：`member`（默认，可通过 `DB_NAME` 环境变量覆盖）

> 使用 mysql-mcp 工具执行以下 SQL。若找不到 mysql-mcp，使用 `tool_search_tool_regex` 搜索 `mysql|sql.*query|execute.*sql`。

---

### <a name="register"></a>A1 — 注册后验证

**验证：member_member 表中存在新用户行**

```sql
SELECT
    member_id,
    nickname,
    email,
    phone,
    status,
    gmt_create
FROM member.member_member
WHERE nickname = '{NICKNAME}'
ORDER BY gmt_create DESC
LIMIT 1;
```

**期望结果：**

- 返回 1 行
- `status = 'ACTIVE'`
- `member_id` 不为 null（记录为 MEMBER_ID）
- `gmt_create` 在当前时间的前后 30 秒内

---

### <a name="login"></a>A2 — 登录后验证

**验证：登录尝试记录**

```sql
SELECT
    member_id,
    success,
    gmt_create
FROM member.member_login_attempt
WHERE member_id = {MEMBER_ID}
ORDER BY gmt_create DESC
LIMIT 3;
```

**期望结果：**

- 最新一行的 `success = 1`（登录成功）

> 若 `member_login_attempt` 表不存在，此查询标记为 `[SKIP - table not found]`。

---

### <a name="update-profile"></a>M4 — 更新资料后验证

**验证：nickname 和 profile 字段已更新**

```sql
SELECT
    member_id,
    nickname,
    first_name,
    last_name,
    gender,
    bio,
    country_code,
    timezone,
    locale,
    gmt_modified
FROM member.member_member
WHERE member_id = {MEMBER_ID};
```

**期望结果：**

- `nickname = '{NICKNAME}_upd'`（或请求中设置的值）
- `first_name = 'Test'`
- `last_name = 'User'`
- `gender = 1`
- `bio = 'Integration test bio'`
- `country_code = 'CN'`
- `timezone = 'Asia/Shanghai'`
- `locale = 'zh_CN'`
- `gmt_modified` 比 `gmt_create` 更新

---

### <a name="change-password"></a>M5 — 修改密码后验证

**验证：password_hash 已变更（操作前需记录旧值）**

```sql
-- 步骤 1：修改密码前执行，记录旧 hash（在 Step 9b 前执行）
SELECT password_hash FROM member.member_member WHERE member_id = {MEMBER_ID};

-- 步骤 2：修改密码后执行，验证 hash 已更改
SELECT
    member_id,
    password_hash,
    gmt_modified
FROM member.member_member
WHERE member_id = {MEMBER_ID};
```

**期望结果：**

- 步骤 2 中的 `password_hash` 与步骤 1 不同
- `gmt_modified` 已更新

---

### <a name="lock"></a>M6 — 锁定账户后验证

**验证：status 变为 LOCKED**

```sql
SELECT
    member_id,
    status,
    gmt_modified
FROM member.member_member
WHERE member_id = {MEMBER_ID};
```

**期望结果：**

- `status = 'LOCKED'`

---

### <a name="unlock"></a>M7 — 解锁账户后验证

**验证：status 恢复为 ACTIVE**

```sql
SELECT
    member_id,
    status,
    gmt_modified
FROM member.member_member
WHERE member_id = {MEMBER_ID};
```

**期望结果：**

- `status = 'ACTIVE'`

---

## Redis 验证

Redis 默认：host=localhost, port=6379, db=0

> 使用 redis-mcp 工具执行以下命令。若找不到 redis-mcp，使用 `tool_search_tool_regex` 搜索 `redis|cache.*get|redis.*key`。

---

### <a name="login"></a>登录后 — Token 黑名单不存在

**验证：当前 token 未在黑名单中**

```
EXISTS auth:blacklist:{TOKEN_ID}
```

**期望结果：** `0`（key 不存在，token 有效）

---

### <a name="logout"></a>A4 — 登出后 — Token 加入黑名单

**验证：token 已加入黑名单**

```
EXISTS auth:blacklist:{TOKEN_ID}
GET auth:blacklist:{TOKEN_ID}
TTL auth:blacklist:{TOKEN_ID}
```

**期望结果：**

- `EXISTS` 返回 `1`（key 存在）
- `TTL` 返回正数（key 设置了过期时间，应小于原 accessToken 有效期）

---

### <a name="member-read"></a>M3 — 权限查询后 — 权限版本缓存

**验证：权限版本缓存 key 存在**

```
GET auth:perm:version:{USER_DOMAIN}:{MEMBER_ID}
TTL auth:perm:version:{USER_DOMAIN}:{MEMBER_ID}
```

**期望结果：**

- `GET` 返回一个版本号字符串（不为 nil）
- `TTL` 返回正数

> 若 key 不存在，可能是权限缓存未触发（新用户无角色权限），标记为 `[INFO - no permissions cached, expected for new user]` 而非 FAIL。

---

### 验证码相关 Key（信息项，非测试接口触发）

以下 key 仅在调用验证码接口时产生，本次测试未覆盖验证码接口，仅供参考：

```
# 验证码存储
KEYS verification_code:*

# 发送频率限制
KEYS verification_rate:*

# 失败计数
KEYS verification_failure:*
```

---

## RabbitMQ 验证

<a name="rabbitmq"></a>

> 使用 rabbitmq-mcp 工具检查。若找不到 rabbitmq-mcp，使用 `tool_search_tool_regex` 搜索 `rabbit|rabbitmq|amqp|message.*queue`。

RabbitMQ 中 domain event 的具体 exchange/queue 名称取决于 member-service 的配置。按以下步骤发现：

### Step 1 — 列出所有 Queue

使用 rabbitmq-mcp 列出当前所有 queue，找出包含 `member` 或 `event` 关键字的 queue：

```
# 列出所有队列（使用 rabbitmq-mcp 的 list_queues 或等价工具）
list_queues()
```

### Step 2 — 检查写操作后的消息

对以下写操作，每次执行后检查相关 queue 中是否有新消息：

| 操作        | 期望的 event 类型       | 检查 queue（模糊匹配）            |
| ----------- | ----------------------- | --------------------------------- |
| M4 更新资料 | `MemberProfileUpdated`  | `member.*event\|member.*profile`  |
| M5 修改密码 | `MemberPasswordChanged` | `member.*event\|member.*password` |
| M6 锁定账户 | `MemberLocked`          | `member.*event\|member.*lock`     |
| M7 解锁账户 | `MemberUnlocked`        | `member.*event\|member.*unlock`   |

### Step 3 — 验证消息内容

```
# 从队列中 peek 最新消息（不 ack，使用 get_message 或等价工具）
get_message(queue="{queue_name}", count=1, ack=false)
```

**期望结果：**

- 消息 payload 中包含操作对应的 `memberId = {MEMBER_ID}`
- 消息 `eventType` 字段值与表格期望一致

> 若所有 queue 都为空或不存在 member 相关 queue，标记为 `[INFO - no domain events found, verify RabbitMQ config]`。
