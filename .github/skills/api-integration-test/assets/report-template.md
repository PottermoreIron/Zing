# API 集成测试报告

**日期：** {YYYY-MM-DD}
**运行时间：** {HH:MM:SS}
**执行环境：** {auth-service: http://localhost:8081 | member-service: http://localhost:11000}
**测试用户：** nickname=`{NICKNAME}` | memberId=`{MEMBER_ID}`

---

## MCP 工具可用性

| 工具         | 状态                                    |
| ------------ | --------------------------------------- |
| mysql-mcp    | ✅ 可用 / ⚠️ 不可用（DB 验证已跳过）    |
| redis-mcp    | ✅ 可用 / ⚠️ 不可用（Redis 验证已跳过） |
| rabbitmq-mcp | ✅ 可用 / ⚠️ 不可用（MQ 验证已跳过）    |

---

## 测试摘要

| #   | 接口                                     | HTTP          | DB          | Redis          | MQ          | 结论             |
| --- | ---------------------------------------- | ------------- | ----------- | -------------- | ----------- | ---------------- |
| A1  | POST /api/v1/register                    | {HTTP_STATUS} | {DB_RESULT} | —              | —           | {PASS/FAIL/SKIP} |
| A2  | POST /api/v1/login                       | {HTTP_STATUS} | {DB_RESULT} | {REDIS_RESULT} | —           | {PASS/FAIL/SKIP} |
| A3  | POST /api/v1/refresh                     | {HTTP_STATUS} | —           | —              | —           | {PASS/FAIL/SKIP} |
| A4  | POST /api/v1/logout                      | {HTTP_STATUS} | —           | {REDIS_RESULT} | —           | {PASS/FAIL/SKIP} |
| M1  | GET /api/members/me                      | {HTTP_STATUS} | —           | —              | —           | {PASS/FAIL/SKIP} |
| M2  | GET /api/members/{MEMBER_ID}             | {HTTP_STATUS} | —           | —              | —           | {PASS/FAIL/SKIP} |
| M3  | GET /api/members/{MEMBER_ID}/permissions | {HTTP_STATUS} | —           | {REDIS_RESULT} | —           | {PASS/FAIL/SKIP} |
| M4  | PUT /api/members/{MEMBER_ID}/profile     | {HTTP_STATUS} | {DB_RESULT} | —              | {MQ_RESULT} | {PASS/FAIL/SKIP} |
| M5  | PUT /api/members/{MEMBER_ID}/password    | {HTTP_STATUS} | {DB_RESULT} | —              | {MQ_RESULT} | {PASS/FAIL/SKIP} |
| M6  | POST /api/members/{MEMBER_ID}/lock       | {HTTP_STATUS} | {DB_RESULT} | —              | {MQ_RESULT} | {PASS/FAIL/SKIP} |
| M7  | POST /api/members/{MEMBER_ID}/unlock     | {HTTP_STATUS} | {DB_RESULT} | —              | {MQ_RESULT} | {PASS/FAIL/SKIP} |

**总计：** {TOTAL} 项 | ✅ 通过：{PASS_COUNT} | ❌ 失败：{FAIL_COUNT} | ⏭️ 跳过：{SKIP_COUNT}

---

## 详细结果

---

### A1 — POST /api/v1/register（注册）

**curl 命令：**

```bash
curl -s -X POST http://localhost:8081/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{"registerType":"USERNAME_PASSWORD","nickname":"{NICKNAME}","password":"[REDACTED]","userDomain":"USER"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}
**响应 code：** {RESPONSE_CODE}
**userId：** {MEMBER_ID}

**DB 验证：**

```sql
SELECT member_id, nickname, status, gmt_create FROM member.member_member WHERE nickname = '{NICKNAME}';
```

| member_id   | nickname   | status | gmt_create  |
| ----------- | ---------- | ------ | ----------- |
| {MEMBER_ID} | {NICKNAME} | ACTIVE | {TIMESTAMP} |

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### A2 — POST /api/v1/login（登录）

**curl 命令：**

```bash
curl -s -X POST http://localhost:8081/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":"USERNAME_PASSWORD","nickname":"{NICKNAME}","password":"[REDACTED]","userDomain":"USER"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}
**accessToken（前 30 字符）：** {TOKEN_PREFIX}...

**Redis 验证（黑名单 key 不存在）：**

```
EXISTS auth:blacklist:{TOKEN_ID}
=> 0 (key 不存在，token 有效)
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### A3 — POST /api/v1/refresh（Token 刷新）

**curl 命令：**

```bash
curl -s -X POST http://localhost:8081/api/v1/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"[REDACTED]"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}
**新 accessToken（前 30 字符）：** {NEW_TOKEN_PREFIX}...（与旧 token 不同：{DIFF_CHECK}）

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### A4 — POST /api/v1/logout（登出）

**curl 命令：**

```bash
curl -s -X POST http://localhost:8081/api/v1/logout \
  -H "Authorization: Bearer [REDACTED]" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"[REDACTED]"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**Redis 验证（黑名单 key 存在）：**

```
EXISTS auth:blacklist:{TOKEN_ID}
=> 1 (key 存在，token 已吊销)

TTL auth:blacklist:{TOKEN_ID}
=> {TTL_SECONDS} 秒
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M1 — GET /api/members/me

**curl 命令：**

```bash
curl -s -X GET http://localhost:11000/api/members/me \
  -H "Authorization: Bearer [REDACTED]"
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}
**返回 memberId：** {RETURNED_MEMBER_ID}（期望：{MEMBER_ID}）

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M2 — GET /api/members/{MEMBER_ID}

**curl 命令：**

```bash
curl -s -X GET http://localhost:11000/api/members/{MEMBER_ID} \
  -H "Authorization: Bearer [REDACTED]"
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M3 — GET /api/members/{MEMBER_ID}/permissions

**curl 命令：**

```bash
curl -s -X GET http://localhost:11000/api/members/{MEMBER_ID}/permissions \
  -H "Authorization: Bearer [REDACTED]"
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}
**权限数量：** {PERMISSION_COUNT}

**Redis 验证（权限版本 key）：**

```
GET auth:perm:version:USER:{MEMBER_ID}
=> {VERSION_VALUE} / (nil)

TTL auth:perm:version:USER:{MEMBER_ID}
=> {TTL_SECONDS} 秒
```

**结论：** ✅ PASS / ❌ FAIL / ℹ️ INFO — {说明}

---

### M4 — PUT /api/members/{MEMBER_ID}/profile（更新资料）

**curl 命令：**

```bash
curl -s -X PUT http://localhost:11000/api/members/{MEMBER_ID}/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [REDACTED]" \
  -d '{"nickname":"{NICKNAME}_upd","firstName":"Test","lastName":"User","gender":1,"bio":"Integration test bio","countryCode":"CN","timezone":"Asia/Shanghai","locale":"zh_CN"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**DB 验证：**

```sql
SELECT nickname, first_name, last_name, gender, bio, country_code, timezone, locale, gmt_modified
FROM member.member_member WHERE member_id = {MEMBER_ID};
```

| 字段         | 期望值               | 实际值   | 一致     |
| ------------ | -------------------- | -------- | -------- |
| nickname     | {NICKNAME}\_upd      | {ACTUAL} | {YES/NO} |
| first_name   | Test                 | {ACTUAL} | {YES/NO} |
| last_name    | User                 | {ACTUAL} | {YES/NO} |
| gender       | 1                    | {ACTUAL} | {YES/NO} |
| bio          | Integration test bio | {ACTUAL} | {YES/NO} |
| country_code | CN                   | {ACTUAL} | {YES/NO} |
| timezone     | Asia/Shanghai        | {ACTUAL} | {YES/NO} |
| locale       | zh_CN                | {ACTUAL} | {YES/NO} |

**RabbitMQ 验证：**

```
Queue: {QUEUE_NAME}
Event type: MemberProfileUpdated
memberId: {MEMBER_ID}
=> {消息内容 / 未找到消息}
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M5 — PUT /api/members/{MEMBER_ID}/password（修改密码）

**curl 命令：**

```bash
curl -s -X PUT http://localhost:11000/api/members/{MEMBER_ID}/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [REDACTED]" \
  -d '{"oldPassword":"[REDACTED]","newPassword":"[REDACTED]"}'
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**DB 验证（password_hash 已变更）：**

|               | 旧 hash（前 20 字符） | 新 hash（前 20 字符） | 已变更   |
| ------------- | --------------------- | --------------------- | -------- |
| password_hash | {OLD_HASH_PREFIX}...  | {NEW_HASH_PREFIX}...  | {YES/NO} |

**RabbitMQ 验证：**

```
Queue: {QUEUE_NAME}
Event type: MemberPasswordChanged
memberId: {MEMBER_ID}
=> {消息内容 / 未找到消息}
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M6 — POST /api/members/{MEMBER_ID}/lock（锁定）

**curl 命令：**

```bash
curl -s -X POST http://localhost:11000/api/members/{MEMBER_ID}/lock \
  -H "Authorization: Bearer [REDACTED]"
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**DB 验证：**

```sql
SELECT status FROM member.member_member WHERE member_id = {MEMBER_ID};
```

**status：** {ACTUAL_STATUS}（期望：`LOCKED`）

**RabbitMQ 验证：**

```
Event type: MemberLocked
=> {消息内容 / 未找到消息}
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

### M7 — POST /api/members/{MEMBER_ID}/unlock（解锁）

**curl 命令：**

```bash
curl -s -X POST http://localhost:11000/api/members/{MEMBER_ID}/unlock \
  -H "Authorization: Bearer [REDACTED]"
```

**原始响应：**

```json
{响应内容}
```

**HTTP 状态码：** {STATUS_CODE}

**DB 验证：**

```sql
SELECT status FROM member.member_member WHERE member_id = {MEMBER_ID};
```

**status：** {ACTUAL_STATUS}（期望：`ACTIVE`）

**RabbitMQ 验证：**

```
Event type: MemberUnlocked
=> {消息内容 / 未找到消息}
```

**结论：** ✅ PASS / ❌ FAIL — {失败原因}

---

## 失败汇总

> 若无失败项，此节为空。

| #   | 接口 | 失败类型 | 详情 |
| --- | ---- | -------- | ---- |
| -   | -    | -        | -    |

---

## 测试数据清理

以下 SQL 可手动执行，删除本次测试产生的数据：

```sql
-- 清理测试用户（替换为实际 MEMBER_ID）
DELETE FROM member.member_member WHERE member_id = {MEMBER_ID};
DELETE FROM member.member_device WHERE member_id = {MEMBER_ID};
DELETE FROM member.member_member_role WHERE member_id = {MEMBER_ID};

-- 验证清理完成
SELECT COUNT(*) FROM member.member_member WHERE member_id = {MEMBER_ID};
```

---

## 已知限制与手动验证项

- **验证码接口（/auth/email, /auth/sms）**：需要真实邮箱/手机号，已排除出自动测试范围
- **内部接口（/internal/**）\*\*：为服务间通信设计，需要内部网络，已排除
- **RabbitMQ 消息**：消费者可能在检查前已消费消息；若 MQ 验证标记为 not found，建议检查消费者日志
- **密码修改后的旧密码登录验证**：Step 9b 后建议手动使用旧密码尝试登录，确认返回 401
