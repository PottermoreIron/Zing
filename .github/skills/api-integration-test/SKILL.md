---
name: api-integration-test
description: "API 集成测试工作流：自动 curl 所有接口、通过 mysql-mcp/redis-mcp/rabbitmq-mcp 验证数据库和缓存状态、生成 Markdown 测试报告。Use when testing APIs, verifying endpoints, checking database state after API calls, curl testing, integration testing with middleware, validating Redis cache, checking RabbitMQ events. 触发词：API测试、接口验证、curl测试、集成测试、mysql-mcp、redis-mcp、rabbitmq-mcp、测试报告、验证接口、接口正确性、中间件验证。"
argument-hint: "可选：指定要测试的模块（auth/member/all，默认 all）或 base URL（如 http://localhost:8081）"
---

# API 集成测试工作流

## 概述

此 skill 执行端到端 API 集成测试：

1. 确认 MCP 工具可用性（mysql-mcp、redis-mcp、rabbitmq-mcp）
2. 从 OpenAPI `/v3/api-docs` 动态获取接口列表
3. 生成唯一测试数据（时间戳后缀），避免与已有数据冲突
4. 按依赖顺序执行接口：注册 → 登录 → Auth 接口 → Member 接口
5. 每个接口执行后立即通过 mysql-mcp / redis-mcp / rabbitmq-mcp 验证数据层状态
6. 将所有结果汇总写入 `docs/api-test-report-{YYYY-MM-DD}.md`

## MCP 依赖声明

运行此 skill 前，需要在 MCP 配置中启用以下服务（任意一项缺失时，对应验证层标记为 `[SKIP]`，不影响其他层）：

| MCP          | 用途                 | 搜索 pattern                             |
| ------------ | -------------------- | ---------------------------------------- |
| mysql-mcp    | 验证 DB 行变化       | `mysql\|sql.*query\|execute.*sql`        |
| redis-mcp    | 验证缓存 key 存在/值 | `redis\|cache.*get\|redis.*key`          |
| rabbitmq-mcp | 验证事件入队         | `rabbit\|rabbitmq\|amqp\|message.*queue` |

## 服务基础 URL

| 服务           | 默认地址                 |
| -------------- | ------------------------ |
| auth-service   | `http://localhost:8081`  |
| member-service | `http://localhost:11000` |

## 执行步骤（10 步）

详细步骤见 [procedure.md](./references/procedure.md)：

- **Step 1** — Pre-flight：检查服务健康状态 + MCP 工具可用性
- **Step 2** — OpenAPI 发现：fetch `/v3/api-docs` 确认接口列表
- **Step 3** — 测试数据生成：生成唯一 `nickname`、`email` 带时间戳
- **Step 4** — 注册测试：`POST /api/v1/register` + DB 验证
- **Step 5** — 登录 Bootstrap：`POST /api/v1/login` 提取 token + Redis 验证
- **Step 6** — Token 刷新测试：`POST /api/v1/refresh` + Redis 验证
- **Step 7** — 登出测试：`POST /api/v1/logout` + Redis 黑名单验证
- **Step 8** — Member 读接口测试：GET /me + GET /{id} + GET /{id}/permissions + Redis 缓存验证
- **Step 9** — Member 写接口测试：PUT /profile + PUT /password + POST /lock + POST /unlock + DB 验证 + RabbitMQ 事件验证
- **Step 10** — 报告生成：写入 `docs/api-test-report-{YYYY-MM-DD}.md`

## 接口测试数据参考

每个接口的请求体、预期状态码、响应断言字段见 [endpoint-test-data.md](./references/endpoint-test-data.md)。

## 数据验证查询参考

每个操作对应的 MySQL 查询、Redis 命令、RabbitMQ 检查见 [verification-queries.md](./references/verification-queries.md)。

## 报告格式模板

输出报告格式见 [report-template.md](./assets/report-template.md)。

## 测试数据清理

此 skill **不**自动删除测试数据（破坏性操作需人工审批）。测试完成后，报告中会包含生成的 `memberId` 和 `nickname`，供手动清理：

```sql
-- member DB 中手动清理（替换 {memberId}）
DELETE FROM member_member WHERE member_id = {memberId};
```
