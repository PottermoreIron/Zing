# Admin 管理平台 — 产品需求文档

> 版本：1.0 | 日期：2026-04-13

---

## 1. 产品定位

Admin 是 Zing 平台的**统一管理后台**，面向内部运营、客服和管理员，提供用户管理、权限控制、系统运营、数据分析和内容审核能力。

**核心设计理念：多应用管理平台**

Admin 不仅管理现有的 member、im 服务，还具备扩展能力，未来可纳管 mall、music、video 等新业务线。每个业务线在 Admin 中作为独立「应用」注册，Admin 提供统一的管理视角和权限隔离。

```
┌──────────────────────────────────────────────┐
│              Admin 管理平台                    │
├──────────┬──────────┬──────────┬─────────────┤
│  Member  │    IM    │   Mall   │  Music ...  │
│  管理     │  管理    │  管理    │   管理       │
└──────────┴──────────┴──────────┴─────────────┘
```

---

## 2. 目标用户

| 角色           | 描述                     | 典型操作                                  |
| -------------- | ------------------------ | ----------------------------------------- |
| **超级管理员** | 平台所有者，拥有全部权限 | 管理员账号管理、全局配置、权限策略制定    |
| **运营管理员** | 负责日常运营             | 用户管理、数据看板、公告发布、内容审核    |
| **客服专员**   | 处理用户问题             | 查看用户信息、处理封禁/解封、查看操作日志 |
| **审计员**     | 安全与合规               | 查看审计日志、权限变更记录、安全事件      |
| **应用管理员** | 特定应用负责人           | 管理其负责应用范围内的资源                |

---

## 3. 功能总览与分期

### Phase 1 — 基础管理能力（MVP）

| 功能模块         | 优先级 | 描述                                       |
| ---------------- | ------ | ------------------------------------------ |
| 管理员认证       | P0     | 管理员登录、登出、Token 刷新               |
| 管理员账号管理   | P0     | 管理员 CRUD、重置密码、启用/禁用           |
| 管理员 RBAC      | P0     | 管理员角色与权限管理，控制管理后台访问范围 |
| 会员管理         | P0     | 会员列表、搜索、详情查看、封禁/解封        |
| 平台角色权限管理 | P0     | 平台角色 CRUD、权限分配、用户角色绑定      |
| 操作审计日志     | P0     | 记录所有管理员操作，支持查询与过滤         |

### Phase 2 — 运营工具

| 功能模块     | 优先级 | 描述                                     |
| ------------ | ------ | ---------------------------------------- |
| 系统配置管理 | P1     | 系统参数、功能开关、全局设置             |
| 系统公告     | P1     | 公告的创建、发布、撤回、定时发布         |
| 数据看板     | P1     | 用户统计、活跃度趋势、注册趋势、设备分布 |
| 应用注册管理 | P1     | 管理纳入 Admin 的应用，应用级权限隔离    |

### Phase 3 — 内容管理与多应用扩展

| 功能模块       | 优先级 | 描述                                         |
| -------------- | ------ | -------------------------------------------- |
| IM 内容管理    | P2     | 消息审核、敏感词过滤、群组管理               |
| 多应用资源管理 | P2     | 针对 mall/music/video 等新业务线的管理脚手架 |
| 高级数据分析   | P2     | 用户行为漏斗、留存分析、多维度报表           |

---

## 4. Phase 1 详细需求

### 4.1 管理员认证

**业务规则：**

- 管理员账号与平台会员账号完全隔离，使用独立的 `admin_user` 身份
- 管理员不能通过公开注册创建，只能由超级管理员在后台创建
- 认证流程复用 auth-service 的 JWT 基础设施，admin 作为独立认证域（domain）
- 管理员 JWT Token 的有效期应短于普通用户（建议 access_token 2h，refresh_token 12h）

**用户故事：**

- 作为管理员，我可以使用用户名+密码登录管理后台
- 作为管理员，我可以安全登出，Token 立即失效
- 作为管理员，我可以刷新 Token 以保持登录状态

**接口概要：**

| 方法 | 路径                    | 描述               |
| ---- | ----------------------- | ------------------ |
| POST | `/admin/api/v1/login`   | 管理员登录         |
| POST | `/admin/api/v1/logout`  | 管理员登出         |
| POST | `/admin/api/v1/refresh` | 刷新 Token         |
| GET  | `/admin/api/v1/me`      | 获取当前管理员信息 |

### 4.2 管理员账号管理

**业务规则：**

- 只有拥有 `admin:user:write` 权限的管理员可以创建/编辑管理员账号
- 超级管理员角色不可删除，不可修改自身角色
- 管理员状态：`active`（正常）、`disabled`（禁用）、`locked`（锁定，登录失败过多）
- 管理员账号使用独立的 ID 生成器（Leaf）

**用户故事：**

- 作为超级管理员，我可以创建新的管理员账号并分配角色
- 作为超级管理员，我可以禁用/启用管理员账号
- 作为管理员，我可以修改自己的密码
- 作为超级管理员，我可以重置其他管理员的密码

**接口概要：**

| 方法 | 路径                                      | 描述               |
| ---- | ----------------------------------------- | ------------------ |
| GET  | `/admin/api/v1/admin-users`               | 管理员列表（分页） |
| POST | `/admin/api/v1/admin-users`               | 创建管理员         |
| GET  | `/admin/api/v1/admin-users/{id}`          | 管理员详情         |
| PUT  | `/admin/api/v1/admin-users/{id}`          | 更新管理员信息     |
| PUT  | `/admin/api/v1/admin-users/{id}/status`   | 更改管理员状态     |
| PUT  | `/admin/api/v1/admin-users/{id}/password` | 重置管理员密码     |

### 4.3 管理员 RBAC

**业务规则：**

- 管理员权限码格式：`{app}:{resource}:{action}`，天然支持多应用隔离
  - 例：`member:user:read`、`im:message:delete`、`admin:config:write`
- 内置系统角色（不可删除）：`super_admin`（全部权限）、`operator`（运营权限）、`auditor`（审计只读）
- 自定义角色可指定任意权限组合
- 权限变更后清除相关管理员的权限缓存

**预置权限清单（Phase 1）：**

| 权限码                    | 描述             | 默认角色              |
| ------------------------- | ---------------- | --------------------- |
| `admin:user:read`         | 查看管理员账号   | super_admin, auditor  |
| `admin:user:write`        | 管理管理员账号   | super_admin           |
| `admin:role:read`         | 查看管理角色     | super_admin, auditor  |
| `admin:role:write`        | 管理管理角色     | super_admin           |
| `admin:audit:read`        | 查看审计日志     | super_admin, auditor  |
| `member:user:read`        | 查看会员信息     | super_admin, operator |
| `member:user:write`       | 管理会员状态     | super_admin, operator |
| `member:role:read`        | 查看平台角色     | super_admin, operator |
| `member:role:write`       | 管理平台角色权限 | super_admin           |
| `member:permission:read`  | 查看平台权限     | super_admin, operator |
| `member:permission:write` | 管理平台权限     | super_admin           |

### 4.4 会员管理

**业务规则：**

- 管理员查看/操作会员数据通过调用 member-service 实现，admin 不直接访问会员数据库
- 支持按昵称、邮箱、手机号、状态、注册日期范围搜索
- 封禁操作需记录原因，解封后原因保留在审计日志
- 查看会员详情包含：基本信息、角色列表、设备列表、社交账号绑定

**用户故事：**

- 作为运营管理员，我可以分页浏览和搜索会员列表
- 作为运营管理员，我可以查看会员的完整信息（含角色、设备、社交绑定）
- 作为运营管理员，我可以封禁违规用户并填写封禁原因
- 作为客服专员，我可以解封用户
- 作为运营管理员，我可以为会员分配/撤销角色

**接口概要：**

| 方法 | 路径                                       | 描述                 |
| ---- | ------------------------------------------ | -------------------- |
| GET  | `/admin/api/v1/members`                    | 会员列表（分页搜索） |
| GET  | `/admin/api/v1/members/{memberId}`         | 会员详情             |
| PUT  | `/admin/api/v1/members/{memberId}/status`  | 修改会员状态         |
| GET  | `/admin/api/v1/members/{memberId}/roles`   | 会员角色列表         |
| PUT  | `/admin/api/v1/members/{memberId}/roles`   | 分配/撤销角色        |
| GET  | `/admin/api/v1/members/{memberId}/devices` | 会员设备列表         |

### 4.5 平台角色权限管理

**业务规则：**

- 管理的是 member-service 中的平台角色和权限（面向普通用户的 RBAC）
- 系统角色（`is_system_role = true`）不允许删除
- 删除角色前需确认无会员绑定，或级联解绑
- 权限变更后自动触发 PermissionChangedEvent，通知 auth-service 更新缓存

**接口概要：**

| 方法   | 路径                                       | 描述                   |
| ------ | ------------------------------------------ | ---------------------- |
| GET    | `/admin/api/v1/roles`                      | 角色列表               |
| POST   | `/admin/api/v1/roles`                      | 创建角色               |
| GET    | `/admin/api/v1/roles/{roleId}`             | 角色详情（含权限列表） |
| PUT    | `/admin/api/v1/roles/{roleId}`             | 更新角色               |
| DELETE | `/admin/api/v1/roles/{roleId}`             | 删除角色               |
| PUT    | `/admin/api/v1/roles/{roleId}/permissions` | 设置角色权限           |
| GET    | `/admin/api/v1/permissions`                | 权限列表               |
| POST   | `/admin/api/v1/permissions`                | 创建权限               |
| PUT    | `/admin/api/v1/permissions/{id}`           | 更新权限               |
| DELETE | `/admin/api/v1/permissions/{id}`           | 删除权限               |

### 4.6 操作审计日志

**业务规则：**

- 所有管理操作自动记录审计日志（通过 AOP 拦截）
- 日志内容包含：操作人、操作时间、操作类型、目标资源、请求详情、结果、IP 地址
- 审计日志只可查询，不可修改或删除
- 支持按操作人、操作类型、目标资源、时间范围过滤
- 敏感字段（如密码）在入库前脱敏

**日志记录维度：**

| 字段     | 描述                     | 示例                                              |
| -------- | ------------------------ | ------------------------------------------------- |
| 操作人   | 执行操作的管理员         | admin_user_id: 10001                              |
| 操作类型 | 具体操作                 | `MEMBER_BANNED`, `ROLE_CREATED`, `CONFIG_UPDATED` |
| 目标应用 | 所属应用                 | `member`, `im`, `admin`                           |
| 目标资源 | 被操作的资源及ID         | `member:12345`, `role:100`                        |
| 请求详情 | 请求方法、路径、关键参数 | `PUT /admin/api/v1/members/12345/status`          |
| 结果     | 操作结果                 | `SUCCESS`, `FAILED`                               |
| IP 地址  | 操作者IP                 | `192.168.1.100`                                   |

**接口概要：**

| 方法 | 路径                            | 描述                     |
| ---- | ------------------------------- | ------------------------ |
| GET  | `/admin/api/v1/audit-logs`      | 审计日志列表（分页过滤） |
| GET  | `/admin/api/v1/audit-logs/{id}` | 审计日志详情             |

---

## 5. Phase 2 详细需求

### 5.1 系统配置管理

**业务规则：**

- 配置项以 `app_code:category:key` 格式组织
- 支持配置类型：`STRING`、`NUMBER`、`BOOLEAN`、`JSON`
- 配置变更需记录审计日志
- 支持配置项说明、默认值、值约束（如最大值/最小值、枚举范围）
- 关键配置变更后可通过事件通知相关服务刷新

**预置配置项（示例）：**

| 配置键                           | 类型    | 默认值 | 描述                         |
| -------------------------------- | ------- | ------ | ---------------------------- |
| `auth:token:access_ttl_minutes`  | NUMBER  | 120    | Access Token 有效期（分钟）  |
| `auth:token:refresh_ttl_hours`   | NUMBER  | 12     | Refresh Token 有效期（小时） |
| `member:registration:enabled`    | BOOLEAN | true   | 是否开放注册                 |
| `member:login:max_fail_attempts` | NUMBER  | 5      | 最大登录失败次数             |
| `im:message:max_length`          | NUMBER  | 5000   | 单条消息最大长度             |

**接口概要：**

| 方法   | 路径                          | 描述                        |
| ------ | ----------------------------- | --------------------------- |
| GET    | `/admin/api/v1/configs`       | 配置列表（按应用/分类过滤） |
| GET    | `/admin/api/v1/configs/{key}` | 获取单项配置                |
| PUT    | `/admin/api/v1/configs/{key}` | 更新配置值                  |
| POST   | `/admin/api/v1/configs`       | 创建配置项                  |
| DELETE | `/admin/api/v1/configs/{key}` | 删除配置项                  |

### 5.2 系统公告

**业务规则：**

- 公告状态流转：`draft` → `published` → `recalled`（可选 `expired`）
- 支持定时发布（`gmt_publish_at`）和定时过期（`gmt_expire_at`）
- 公告可指定目标应用范围（全平台 / 特定应用）
- 公告类型：`notification`（通知）、`maintenance`（维护公告）、`update`（更新说明）、`warning`（警告）

**接口概要：**

| 方法 | 路径                                       | 描述     |
| ---- | ------------------------------------------ | -------- |
| GET  | `/admin/api/v1/announcements`              | 公告列表 |
| POST | `/admin/api/v1/announcements`              | 创建公告 |
| GET  | `/admin/api/v1/announcements/{id}`         | 公告详情 |
| PUT  | `/admin/api/v1/announcements/{id}`         | 编辑公告 |
| PUT  | `/admin/api/v1/announcements/{id}/publish` | 发布公告 |
| PUT  | `/admin/api/v1/announcements/{id}/recall`  | 撤回公告 |

### 5.3 数据看板

**业务规则：**

- 数据通过调用各服务的内部统计接口聚合（admin 不直接查询其他服务的数据库）
- 支持时间范围选择（今日、本周、本月、自定义）
- 数据可缓存（Redis），缓存时间根据指标类型不同（实时指标 1min，趋势指标 10min）

**指标清单：**

| 指标         | 数据源         | 描述                                         |
| ------------ | -------------- | -------------------------------------------- |
| 总注册用户数 | member-service | 累计注册会员数                               |
| 今日新增用户 | member-service | 今日注册会员数                               |
| 活跃用户数   | member-service | 近7日/30日有登录的用户数                     |
| 用户状态分布 | member-service | active/inactive/suspended/pending 各状态占比 |
| 注册趋势图   | member-service | 近30天每日注册量折线图                       |
| 设备类型分布 | member-service | mobile/desktop/tablet 占比                   |
| 在线用户数   | im-service     | 当前在线连接数                               |
| 今日消息量   | im-service     | 今日发送消息总数                             |

### 5.4 应用注册管理

**业务规则：**

- 每个受管应用注册时指定 `app_code`（如 `member`、`im`、`mall`）
- 应用信息包含：名称、描述、状态、基础 URL、Nacos 服务名
- 应用权限自动以 `app_code:` 前缀隔离
- 新应用注册后，Admin 可为其配置管理权限和管理员角色

**接口概要：**

| 方法 | 路径                                          | 描述          |
| ---- | --------------------------------------------- | ------------- |
| GET  | `/admin/api/v1/applications`                  | 应用列表      |
| POST | `/admin/api/v1/applications`                  | 注册应用      |
| PUT  | `/admin/api/v1/applications/{appCode}`        | 更新应用信息  |
| PUT  | `/admin/api/v1/applications/{appCode}/status` | 启用/停用应用 |

---

## 6. Phase 3 详细需求

### 6.1 IM 内容管理

- 消息审核：查看、搜索用户消息，敏感词标记，删除违规消息
- 群组管理：查看群列表，解散群组，移除群成员
- 敏感词库管理：维护敏感词列表，支持正则匹配

### 6.2 多应用资源管理

- 为每个新应用提供管理脚手架
- 自动生成应用级权限集
- 应用管理员只能看到自己应用范围内的数据和操作

### 6.3 高级数据分析

- 用户留存分析（日/周/月留存）
- 行为漏斗（注册→激活→活跃→付费）
- 自定义报表导出

---

## 7. 非功能性需求

| 维度         | 要求                                                                                    |
| ------------ | --------------------------------------------------------------------------------------- |
| **安全性**   | 管理员身份独立于普通用户；所有操作记录审计日志；敏感数据脱敏；支持 IP 白名单（Phase 2） |
| **性能**     | 列表查询 P99 < 500ms；看板数据缓存 + 异步刷新                                           |
| **可用性**   | 独立部署，不影响面向用户的服务可用性                                                    |
| **可扩展性** | 多应用权限码设计天然支持新应用接入；Plugin 化的应用管理适配器                           |
| **可审计**   | 全操作审计，日志不可篡改，满足合规要求                                                  |
| **兼容性**   | 前端可对接主流 Admin UI 框架（如 Ant Design Pro、Vue Admin）                            |
