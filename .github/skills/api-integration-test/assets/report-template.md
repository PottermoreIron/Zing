# API Integration Test Report

**Date:** {YYYY-MM-DD} | **Test user:** `{NICKNAME}` / `{MEMBER_ID}`
**Services:** auth=`{AUTH_BASE}` | member=`{MEMBER_BASE}`

## MCP Availability

| MCP          | Status                                               |
| ------------ | ---------------------------------------------------- |
| mysql-mcp    | ✅ Available / ⚠️ Unavailable (DB checks skipped)    |
| redis-mcp    | ✅ Available / ⚠️ Unavailable (Redis checks skipped) |
| rabbitmq-mcp | ✅ Available / ⚠️ Unavailable (MQ checks skipped)    |

## Summary

<!-- One row per endpoint. Use actual values. -->

| #    | Endpoint        | HTTP   | DB                 | Redis              | MQ                 | Result                   |
| ---- | --------------- | ------ | ------------------ | ------------------ | ------------------ | ------------------------ |
| {ID} | {METHOD} {PATH} | {CODE} | {PASS/FAIL/SKIP/—} | {PASS/FAIL/SKIP/—} | {PASS/FAIL/SKIP/—} | {PASS/FAIL/SKIP/BLOCKED} |

**Total:** {N} | ✅ {PASS} | ❌ {FAIL} | ⏭️ {SKIP}

## Details

<!-- Repeat this block per endpoint. Redact tokens/passwords in curl commands. -->

### {ID} — {METHOD} {PATH}

**Request:** `curl -s -X {METHOD} {URL} ...` (passwords redacted)

**Response:** HTTP {CODE}

```json
{response body}
```

**Assertions:** {list each assertion → PASS/FAIL with reason}

**Verification:**

- DB: {query + result or SKIP}
- Redis: {command + result or SKIP}
- MQ: {peek result or SKIP}

**Result:** ✅ PASS / ❌ FAIL — {reason}

---

## Cleanup

```sql
-- Replace {MEMBER_ID} with actual value
DELETE FROM member.member_member WHERE member_id = {MEMBER_ID};
```

- **RabbitMQ 消息**：消费者可能在检查前已消费消息；若 MQ 验证标记为 not found，建议检查消费者日志
- **密码修改后的旧密码登录验证**：Step 9b 后建议手动使用旧密码尝试登录，确认返回 401
