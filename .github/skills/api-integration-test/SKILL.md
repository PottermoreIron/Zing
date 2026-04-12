---
name: api-integration-test
description: "API integration test workflow: discover endpoints from OpenAPI, curl all APIs, verify DB/cache/MQ state via mysql-mcp/redis-mcp/rabbitmq-mcp, generate Markdown report. Use when testing APIs, verifying endpoints, curl testing, integration testing, validating database state, checking Redis cache, verifying RabbitMQ events. Trigger: API test, endpoint validation, curl testing, integration testing, test report, API verification, middleware validation."
argument-hint: "Optional: module scope (auth/member/all, default all) or base URL override"
---

# API Integration Test Workflow

## When to Use

- Verify API correctness after code changes
- End-to-end smoke test of running services
- Validate data-layer side effects (DB rows, cache keys, MQ events)

## Workflow Overview

```
Phase 1  Pre-flight     → health check + MCP discovery
Phase 2  API Discovery  → OpenAPI spec → build endpoint list
Phase 3  Execute & Verify → curl each endpoint, verify via MCP
Phase 4  Report         → write docs/api-test-report-{date}.md
```

---

## Phase 1 — Pre-flight

### 1a. Service Health

Check each service's `/actuator/health`. If unreachable, mark `[BLOCKED]` and stop.

| Service        | Default URL              |
| -------------- | ------------------------ |
| auth-service   | `http://localhost:8081`  |
| member-service | `http://localhost:11000` |

### 1b. MCP Tool Discovery

Use `tool_search_tool_regex` to probe each MCP. Record availability; missing MCP → that verification layer is `[SKIP]`.

| MCP      | Search pattern | Purpose        |
| -------- | -------------- | -------------- |
| MySQL    | `^mcp_mysql`   | DB row changes |
| Redis    | `^mcp_redis`   | Cache keys     |
| RabbitMQ | `^mcp_rabbit`  | Domain events  |

---

## Phase 2 — API Discovery

Fetch OpenAPI specs to build the live endpoint list:

```bash
curl -s ${BASE}/v3/api-docs | python3 -c "
import sys,json
for p,m in json.load(sys.stdin).get('paths',{}).items():
  for v in m: print(f'{v.upper()} {p}')
"
```

If `/v3/api-docs` returns 404, retry with `/v3/api-docs/public`.

Cross-reference discovered endpoints against [test-catalog.md](./references/test-catalog.md) to determine:

- **Cataloged endpoints** — execute with predefined test data and verification queries.
- **Uncataloged endpoints** — attempt basic smoke test (call with minimal/empty body, assert non-5xx).

### Execution Order

Endpoints with data dependencies must run in dependency order:

```
register → login → (auth endpoints) → (member read) → (member write)
```

### Test Data Generation

Generate unique test user per run to avoid collisions:

```
TS       = last 6 digits of Unix timestamp
NICKNAME = test_{TS}
EMAIL    = test_{TS}@zing.test
PASSWORD = Test@12345678
```

---

## Phase 3 — Execute & Verify

For each endpoint, apply these three generic patterns in sequence.

### Pattern A — HTTP Request

```bash
RESP=$(curl -s -w "\n%{http_code}" -X {METHOD} {BASE}{PATH} \
  -H "Content-Type: application/json" \
  ${AUTH:+-H "Authorization: Bearer ${ACCESS_TOKEN}"} \
  ${BODY:+-d "${BODY}"})
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY_JSON=$(echo "$RESP" | sed '$d')
```

Extract fields from JSON response with `python3 -c "import sys,json; ..."`.

### Pattern B — Assertion

For each endpoint's assertion list (from test-catalog.md):

- Check `HTTP_CODE` matches expected status.
- Check JSON field values: `code == 0`, required fields not null, etc.
- Record `[PASS]` or `[FAIL - reason]`.

### Pattern C — Data-Layer Verification

After each request, if the endpoint has verification queries in test-catalog.md:

| Layer | Action                                                             | On MCP missing |
| ----- | ------------------------------------------------------------------ | -------------- |
| DB    | Execute SQL via mysql-mcp `query` tool, compare result to expected | `[SKIP]`       |
| Redis | Execute command via redis-mcp `get`/`scan_keys`/`key_info` tools   | `[SKIP]`       |
| MQ    | Use rabbitmq-mcp `list_queues` + `peek` to check domain events     | `[SKIP]`       |

### Error Handling

- Assertion failure → record `[FAIL]`, **continue** to next endpoint.
- HTTP 5xx → record `[ERROR]`, continue.
- Dependency failure (e.g., register fails) → skip all downstream endpoints, mark `[BLOCKED]`.

---

## Phase 4 — Report

Write results to `docs/api-test-report-{YYYY-MM-DD}.md` using [report-template.md](./assets/report-template.md).

Output the summary table in chat after writing.

### Data Cleanup

This skill does **not** auto-delete test data. The report includes generated `memberId` and `nickname` for manual cleanup.

---

## Reference Files

| File                                              | Content                                                     | When to load                        |
| ------------------------------------------------- | ----------------------------------------------------------- | ----------------------------------- |
| [test-catalog.md](./references/test-catalog.md)   | Per-endpoint: request, assertions, DB/Redis/MQ verification | Phase 3, before executing endpoints |
| [report-template.md](./assets/report-template.md) | Report format pattern                                       | Phase 4, when generating report     |
