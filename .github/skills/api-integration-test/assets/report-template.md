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

- **RabbitMQ messages**: consumers may have already consumed the message before inspection; if MQ verification shows "not found", check consumer logs
- **密码修改后的旧密码登录验证**：Step 9b 后建议手动使用旧密码尝试登录，确认返回 401
