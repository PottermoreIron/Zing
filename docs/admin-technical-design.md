# Admin 管理平台 — 技术方案

> 版本：1.0 | 日期：2026-04-13

---

## 1. 架构总览

### 1.1 定位

admin-service 是独立部署的微服务，采用 DDD/Hexagonal 架构，通过 Feign 调用 member-service/im-service 等下游服务实现资源管理。Admin 拥有自己的数据库（管理员账号、审计日志、系统配置等），不直接访问其他服务的数据库。

### 1.2 系统上下文

```
                         ┌──────────────┐
                         │   Frontend   │
                         │  (Admin UI)  │
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │   Gateway    │
                         │ (JWT + 路由)  │
                         └──────┬───────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
     ┌──────▼───────┐   ┌──────▼───────┐   ┌──────▼───────┐
     │ auth-service  │   │admin-service │   │    其他服务    │
     │  (认证签发)    │   │  (管理后台)   │   │              │
     └──────┬───────┘   └──────┬───────┘   └──────────────┘
            │                   │
            │            ┌──────┴──────────────┐
            │            │                     │
            │     ┌──────▼───────┐     ┌──────▼───────┐
            └────►│member-service│     │  im-service  │
                  │  (用户/RBAC)  │     │ (IM资源管理)  │
                  └──────────────┘     └──────────────┘
```

### 1.3 认证方案（架构决策）

**决策：复用 auth-service，Admin 作为独立认证域**

推荐此方案的原因：

1. **安全集中管理** — 认证逻辑（JWT 签发、刷新、黑名单）集中在 auth-service，不在多个服务中重复实现安全关键代码
2. **复用现有基础设施** — auth-service 的 Strategy 模式、限流、Token 管理已经成熟
3. **一致的 Token 格式** — Gateway 统一验证，无需为 admin 增加额外验证逻辑
4. **工业实践参考** — Keycloak/Auth0 等企业方案均采用中心化认证 + 多域隔离

**认证流程：**

```
Admin UI                  Gateway            auth-service          admin-service
   │                         │                    │                      │
   │  POST /auth/api/v1/login                     │                      │
   │  { username, password,                        │                      │
   │    domain: "admin" }    │                     │                      │
   │────────────────────────►│                     │                      │
   │                         │────────────────────►│                      │
   │                         │                     │  AdminModuleAdapter  │
   │                         │                     │  .findByUsername()   │
   │                         │                     │─────────────────────►│
   │                         │                     │   AdminUserDTO       │
   │                         │                     │◄─────────────────────│
   │                         │                     │                      │
   │                         │                     │  .authenticate()     │
   │                         │                     │─────────────────────►│
   │                         │                     │   验证密码, 返回结果   │
   │                         │                     │◄─────────────────────│
   │                         │                     │                      │
   │                         │  JWT { domain:"admin", admin_user_id,     │
   │                         │        permissions, perm_version }        │
   │                         │◄────────────────────│                      │
   │    JWT Token Pair       │                     │                      │
   │◄────────────────────────│                     │                      │
```

**JWT Token Claims（Admin 域）：**

```json
{
  "sub": "admin_10001",
  "domain": "admin",
  "admin_user_id": 10001,
  "username": "admin",
  "perm_version": 3,
  "perm_digest": "a1b2c3...",
  "iat": 1712000000,
  "exp": 1712007200
}
```

**auth-service 需要的改动：**

- 实现 `AdminModuleAdapter`（已有 stub），通过 Feign 调用 admin-service 内部接口
- `LoginCommand` / `LoginApplicationService` 增加 `domain` 字段路由
- JWT claims 中增加 `domain` 字段
- Token TTL 按 domain 区分（admin: access 2h / refresh 12h）

**Gateway 需要的改动：**

- `/admin/**` 路由到 admin-service
- JWT 验证时解析 `domain` claim，admin 路由要求 `domain = "admin"`

---

## 2. 模块结构

### 2.1 Maven 模块

```
admin/
├── pom.xml                        # 聚合 POM
├── admin-facade/                  # Facade 模块（供其他服务依赖）
│   └── src/main/java/com/pot/admin/facade/
│       ├── api/
│       │   └── InternalAdminFacade.java       # Feign 接口定义
│       ├── dto/
│       │   ├── AdminUserDTO.java
│       │   ├── AdminPermissionDTO.java
│       │   └── request/
│       │       └── AuthenticateAdminRequest.java
│       └── constants/
│           └── AdminConstants.java
│
└── admin-service/                 # 业务实现模块
    └── src/main/java/com/pot/admin/service/
        ├── AdminServiceApplication.java
        │
        ├── interfaces/                    # 接口层：Controller + 请求/响应
        │   ├── rest/
        │   │   ├── AdminUserController.java
        │   │   ├── MemberManagementController.java
        │   │   ├── RoleManagementController.java
        │   │   ├── PermissionManagementController.java
        │   │   ├── AuditLogController.java
        │   │   └── internal/
        │   │       └── InternalAdminController.java
        │   ├── request/
        │   │   ├── CreateAdminUserRequest.java
        │   │   ├── UpdateAdminUserRequest.java
        │   │   ├── UpdateMemberStatusRequest.java
        │   │   ├── AssignMemberRolesRequest.java
        │   │   ├── CreateRoleRequest.java
        │   │   ├── UpdateRoleRequest.java
        │   │   ├── SetRolePermissionsRequest.java
        │   │   ├── CreatePermissionRequest.java
        │   │   └── AuditLogQueryRequest.java
        │   └── exception/
        │       └── GlobalExceptionHandler.java
        │
        ├── application/                   # 应用层：用例编排
        │   ├── service/
        │   │   ├── AdminUserApplicationService.java
        │   │   ├── MemberManagementApplicationService.java
        │   │   ├── RoleManagementApplicationService.java
        │   │   ├── PermissionManagementApplicationService.java
        │   │   └── AuditLogApplicationService.java
        │   ├── command/
        │   │   ├── CreateAdminUserCommand.java
        │   │   ├── UpdateAdminUserCommand.java
        │   │   ├── ChangeAdminPasswordCommand.java
        │   │   ├── UpdateMemberStatusCommand.java
        │   │   └── AssignMemberRolesCommand.java
        │   ├── query/
        │   │   ├── AdminUserQuery.java
        │   │   ├── MemberQuery.java
        │   │   └── AuditLogQuery.java
        │   ├── dto/
        │   │   ├── AdminUserDTO.java
        │   │   ├── AdminRoleDTO.java
        │   │   ├── AdminPermissionDTO.java
        │   │   └── AuditLogDTO.java
        │   └── assembler/
        │       ├── AdminUserAssembler.java
        │       └── AuditLogAssembler.java
        │
        ├── domain/                        # 领域层：核心业务规则
        │   ├── model/
        │   │   ├── admin/
        │   │   │   ├── AdminUser.java             # 聚合根
        │   │   │   ├── AdminUserId.java           # 值对象
        │   │   │   ├── AdminUsername.java          # 值对象
        │   │   │   └── AdminStatus.java           # 值对象/枚举
        │   │   └── audit/
        │   │       ├── AuditLog.java              # 实体
        │   │       ├── AuditLogId.java            # 值对象
        │   │       ├── OperationType.java         # 枚举
        │   │       └── OperationResult.java       # 枚举
        │   ├── service/
        │   │   └── AdminUserDomainService.java
        │   ├── repository/
        │   │   ├── AdminUserRepository.java
        │   │   ├── AdminRoleRepository.java
        │   │   └── AuditLogRepository.java
        │   ├── port/
        │   │   ├── AdminIdGenerator.java
        │   │   ├── PasswordEncoder.java
        │   │   └── PermissionCachePort.java
        │   └── event/
        │       └── AdminPermissionChangedEvent.java
        │
        └── infrastructure/                # 基础设施层：技术实现
            ├── persistence/
            │   ├── entity/
            │   │   ├── AdminUserPO.java
            │   │   ├── AdminRolePO.java
            │   │   ├── AdminPermissionPO.java
            │   │   ├── AdminRolePermissionPO.java
            │   │   ├── AdminUserRolePO.java
            │   │   └── AuditLogPO.java
            │   ├── mapper/
            │   │   ├── AdminUserMapper.java
            │   │   ├── AdminRoleMapper.java
            │   │   ├── AdminPermissionMapper.java
            │   │   ├── AdminRolePermissionMapper.java
            │   │   ├── AdminUserRoleMapper.java
            │   │   └── AuditLogMapper.java
            │   └── repository/
            │       ├── AdminUserRepositoryImpl.java
            │       ├── AdminRoleRepositoryImpl.java
            │       └── AuditLogRepositoryImpl.java
            ├── adapter/
            │   ├── AdminIdGeneratorAdapter.java
            │   ├── BCryptPasswordEncoderAdapter.java
            │   ├── RedisPermissionCacheAdapter.java
            │   └── MemberServiceAdapter.java       # Feign 调用 member-service
            ├── client/
            │   ├── MemberServiceClient.java         # Feign Client
            │   └── ImServiceClient.java             # Feign Client (Phase 3)
            ├── aspect/
            │   └── AuditLogAspect.java              # 审计日志 AOP
            ├── config/
            │   ├── DomainServiceConfig.java
            │   ├── SecurityConfig.java
            │   ├── AdminOpenApiConfig.java
            │   └── FeignClientConfig.java
            └── filter/
                └── AdminContextFilter.java
```

### 2.2 依赖关系

```
admin-facade (被 auth-service 依赖)
    └── framework-common

admin-service
    ├── admin-facade
    ├── member-facade            # Feign 调用 member-service
    ├── framework-common
    ├── framework-starter-authorization   # 权限注解
    ├── framework-starter-redis          # 权限缓存 + 审计日志缓冲
    ├── framework-starter-id             # 分布式 ID
    ├── framework-starter-ratelimit      # 接口限流
    ├── spring-cloud-starter-openfeign   # 服务间调用
    ├── spring-cloud-starter-alibaba-nacos-discovery
    ├── mybatis-plus-spring-boot3-starter
    ├── spring-security-crypto           # 密码加密
    └── mysql-connector-j
```

---

## 3. 领域模型设计

### 3.1 聚合与实体

#### AdminUser 聚合

```java
/**
 * Admin user aggregate root.
 * Manages admin identity, credentials, status, and role assignments.
 */
public class AdminUser {
    private AdminUserId id;
    private AdminUsername username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String passwordHash;
    private AdminStatus status;          // ACTIVE, DISABLED, LOCKED
    private Set<Long> roleIds;           // assigned admin role IDs
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private int loginFailCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behaviors
    public void changePassword(String newEncodedPassword) { ... }
    public void disable() { ... }
    public void enable() { ... }
    public void lock() { ... }
    public void recordLoginSuccess(String ip) { ... }
    public void recordLoginFailure(int maxAttempts) { ... }
    public void assignRole(Long roleId) { ... }
    public void revokeRole(Long roleId) { ... }
}
```

#### AuditLog 实体

```java
/**
 * Immutable audit log entity. Append-only, no update or delete.
 */
public class AuditLog {
    private AuditLogId id;
    private Long adminUserId;
    private String adminUsername;
    private OperationType operationType;
    private String appCode;              // target application
    private String resourceType;         // e.g. "member", "role"
    private String resourceId;           // e.g. "12345"
    private String requestMethod;
    private String requestPath;
    private String requestBody;          // sanitized, sensitive fields masked
    private OperationResult result;      // SUCCESS, FAILED
    private String resultMessage;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
```

### 3.2 值对象

| 值对象            | 描述           | 校验规则                                            |
| ----------------- | -------------- | --------------------------------------------------- |
| `AdminUserId`     | 管理员唯一标识 | 正整数，Leaf 生成                                   |
| `AdminUsername`   | 管理员用户名   | 3-30 字符，字母数字下划线，唯一                     |
| `AdminStatus`     | 账号状态枚举   | `ACTIVE`, `DISABLED`, `LOCKED`                      |
| `AuditLogId`      | 审计日志 ID    | 正整数，Leaf 生成                                   |
| `OperationType`   | 操作类型枚举   | `ADMIN_CREATED`, `MEMBER_BANNED`, `ROLE_CREATED` 等 |
| `OperationResult` | 操作结果枚举   | `SUCCESS`, `FAILED`                                 |

### 3.3 端口定义

| 端口                  | 方向 | 描述              |
| --------------------- | ---- | ----------------- |
| `AdminUserRepository` | 出站 | 管理员持久化      |
| `AdminRoleRepository` | 出站 | 管理员角色持久化  |
| `AuditLogRepository`  | 出站 | 审计日志持久化    |
| `AdminIdGenerator`    | 出站 | 分布式 ID 生成    |
| `PasswordEncoder`     | 出站 | 密码编码/验证     |
| `PermissionCachePort` | 出站 | 权限缓存（Redis） |

---

## 4. 数据库设计

所有表使用 `admin_` 前缀，表名单数形式，MySQL InnoDB + utf8mb4。

### 4.1 admin_user

```sql
CREATE TABLE `admin_user` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`    TIMESTAMP        NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `admin_user_id`     BIGINT UNSIGNED  NOT NULL COMMENT 'Business ID (Leaf)',
    `username`          VARCHAR(30)      NOT NULL COMMENT 'Login username',
    `email`             VARCHAR(255)     NULL COMMENT 'Email',
    `phone`             VARCHAR(20)      NULL COMMENT 'Phone',
    `nickname`          VARCHAR(50)      NULL COMMENT 'Display name',
    `avatar_url`        VARCHAR(500)     NULL COMMENT 'Avatar URL',
    `password_hash`     VARCHAR(255)     NOT NULL COMMENT 'BCrypt hash',
    `status`            ENUM('active','disabled','locked') NOT NULL DEFAULT 'active',
    `login_fail_count`  INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT 'Consecutive login failure count',
    `gmt_last_login_at` TIMESTAMP        NULL COMMENT 'Last login time',
    `last_login_ip`     VARCHAR(45)      NULL COMMENT 'Last login IP',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_user_id` (`admin_user_id`),
    UNIQUE KEY `uk_username` (`username`, `gmt_deleted_at`),
    KEY `idx_email` (`email`, `gmt_deleted_at`),
    KEY `idx_status` (`status`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin user table';
```

### 4.2 admin_role

```sql
CREATE TABLE `admin_role` (
    `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`   TIMESTAMP        NULL     DEFAULT NULL,

    `role_code`        VARCHAR(50)      NOT NULL COMMENT 'Role code: super_admin, operator, auditor',
    `role_name`        VARCHAR(100)     NOT NULL COMMENT 'Display name',
    `role_description` TEXT             NULL,
    `is_system_role`   BOOLEAN          NOT NULL DEFAULT FALSE,
    `is_active`        BOOLEAN          NOT NULL DEFAULT TRUE,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `gmt_deleted_at`),
    KEY `idx_active_roles` (`is_active`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin role table';
```

### 4.3 admin_permission

```sql
CREATE TABLE `admin_permission` (
    `id`                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`         TIMESTAMP        NULL     DEFAULT NULL,

    `permission_code`        VARCHAR(100)     NOT NULL COMMENT 'Code: {app}:{resource}:{action}',
    `permission_name`        VARCHAR(100)     NOT NULL COMMENT 'Display name',
    `permission_description` TEXT             NULL,
    `app_code`               VARCHAR(30)      NOT NULL COMMENT 'Target application code',
    `resource_type`          VARCHAR(50)      NOT NULL COMMENT 'Resource type',
    `action_type`            VARCHAR(50)      NOT NULL COMMENT 'Action: read, write, delete',
    `is_system_permission`   BOOLEAN          NOT NULL DEFAULT FALSE,
    `is_active`              BOOLEAN          NOT NULL DEFAULT TRUE,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`, `gmt_deleted_at`),
    KEY `idx_app_resource` (`app_code`, `resource_type`, `action_type`),
    KEY `idx_active` (`is_active`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin permission table';
```

### 4.4 admin_role_permission

```sql
CREATE TABLE `admin_role_permission` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `gmt_created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at` TIMESTAMP       NULL     DEFAULT NULL,

    `role_id`        BIGINT UNSIGNED NOT NULL,
    `permission_id`  BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`, `gmt_deleted_at`),
    KEY `idx_role` (`role_id`, `gmt_deleted_at`),
    KEY `idx_permission` (`permission_id`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin role-permission association table';
```

### 4.5 admin_user_role

```sql
CREATE TABLE `admin_user_role` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `gmt_created_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`  TIMESTAMP       NULL     DEFAULT NULL,

    `admin_user_id`   BIGINT UNSIGNED NOT NULL COMMENT 'Admin user business ID',
    `role_id`         BIGINT UNSIGNED NOT NULL,
    `is_active`       BOOLEAN         NOT NULL DEFAULT TRUE,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`admin_user_id`, `role_id`, `gmt_deleted_at`),
    KEY `idx_user_roles` (`admin_user_id`, `is_active`, `gmt_deleted_at`),
    KEY `idx_role_users` (`role_id`, `is_active`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin user-role association table';
```

### 4.6 admin_audit_log

```sql
CREATE TABLE `admin_audit_log` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `audit_log_id`    BIGINT UNSIGNED  NOT NULL COMMENT 'Business ID (Leaf)',
    `admin_user_id`   BIGINT UNSIGNED  NOT NULL,
    `admin_username`  VARCHAR(30)      NOT NULL,
    `operation_type`  VARCHAR(50)      NOT NULL COMMENT 'e.g. MEMBER_BANNED, ROLE_CREATED',
    `app_code`        VARCHAR(30)      NOT NULL COMMENT 'Target application',
    `resource_type`   VARCHAR(50)      NOT NULL COMMENT 'Target resource type',
    `resource_id`     VARCHAR(100)     NULL COMMENT 'Target resource ID',
    `request_method`  VARCHAR(10)      NOT NULL,
    `request_path`    VARCHAR(500)     NOT NULL,
    `request_body`    TEXT             NULL COMMENT 'Sanitized request body',
    `result`          ENUM('SUCCESS','FAILED') NOT NULL,
    `result_message`  VARCHAR(500)     NULL,
    `ip_address`      VARCHAR(45)      NOT NULL,
    `user_agent`      VARCHAR(500)     NULL,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_id` (`audit_log_id`),
    KEY `idx_admin_user` (`admin_user_id`, `gmt_created_at`),
    KEY `idx_operation` (`operation_type`, `gmt_created_at`),
    KEY `idx_app_resource` (`app_code`, `resource_type`, `gmt_created_at`),
    KEY `idx_created_at` (`gmt_created_at`),
    KEY `idx_result` (`result`, `gmt_created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Admin audit log table';
```

### 4.7 Phase 2 表（预览）

```sql
-- System configuration
CREATE TABLE `admin_system_config` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`  TIMESTAMP        NULL     DEFAULT NULL,

    `config_key`      VARCHAR(200)     NOT NULL COMMENT 'Key: {app}:{category}:{name}',
    `config_value`    TEXT             NOT NULL,
    `config_type`     ENUM('STRING','NUMBER','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING',
    `app_code`        VARCHAR(30)      NOT NULL,
    `category`        VARCHAR(50)      NOT NULL,
    `description`     VARCHAR(500)     NULL,
    `default_value`   TEXT             NULL,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`, `gmt_deleted_at`),
    KEY `idx_app_category` (`app_code`, `category`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='System configuration table';

-- Announcement
CREATE TABLE `admin_announcement` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`  TIMESTAMP        NULL     DEFAULT NULL,

    `announcement_id` BIGINT UNSIGNED  NOT NULL COMMENT 'Business ID (Leaf)',
    `title`           VARCHAR(200)     NOT NULL,
    `content`         TEXT             NOT NULL,
    `type`            ENUM('notification','maintenance','update','warning') NOT NULL,
    `status`          ENUM('draft','published','recalled','expired') NOT NULL DEFAULT 'draft',
    `target_app`      VARCHAR(30)      NULL COMMENT 'Target app; NULL means all',
    `author_id`       BIGINT UNSIGNED  NOT NULL,
    `gmt_publish_at`  TIMESTAMP        NULL COMMENT 'Scheduled publish time',
    `gmt_expire_at`   TIMESTAMP        NULL COMMENT 'Expiration time',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_announcement_id` (`announcement_id`),
    KEY `idx_status_publish` (`status`, `gmt_publish_at`),
    KEY `idx_target_app` (`target_app`, `status`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='System announcement table';

-- Application registry
CREATE TABLE `admin_application` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `gmt_created_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_updated_at`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `gmt_deleted_at`  TIMESTAMP        NULL     DEFAULT NULL,

    `app_code`        VARCHAR(30)      NOT NULL COMMENT 'Unique application code',
    `app_name`        VARCHAR(100)     NOT NULL,
    `description`     VARCHAR(500)     NULL,
    `base_url`        VARCHAR(255)     NULL COMMENT 'Service base URL',
    `nacos_service`   VARCHAR(100)     NULL COMMENT 'Nacos service name',
    `status`          ENUM('active','disabled') NOT NULL DEFAULT 'active',
    `sort_order`      INT              NOT NULL DEFAULT 0,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_code` (`app_code`, `gmt_deleted_at`),
    KEY `idx_status` (`status`, `gmt_deleted_at`),
    KEY `idx_deleted_at` (`gmt_deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Managed application registry table';
```

---

## 5. API 设计

### 5.1 通用约定

- 所有响应使用 `Result<T>` 统一包装
- 分页使用 `page` + `size` 参数，响应使用 `PageResult<T>`
- 路径前缀：`/admin/api/v1`
- 认证：所有 admin 接口要求 `domain = "admin"` 的 JWT
- 权限：通过 `@RequirePermission` 注解校验

### 5.2 Phase 1 接口详细设计

#### 管理员认证

> 注意：认证走 auth-service，此处仅列出 admin-service 对外提供的管理员信息接口

```yaml
GET /admin/api/v1/me
  summary: 获取当前管理员信息
  permission: (authenticated)
  response: AdminUserDTO { adminUserId, username, nickname, email, roles, permissions }
```

#### 管理员账号管理

```yaml
GET /admin/api/v1/admin-users
  summary: 管理员列表
  permission: admin:user:read
  params: page, size, keyword, status
  response: PageResult<AdminUserDTO>

POST /admin/api/v1/admin-users
  summary: 创建管理员
  permission: admin:user:write
  body: { username, email, phone, nickname, password, roleIds }
  response: AdminUserDTO

GET /admin/api/v1/admin-users/{adminUserId}
  summary: 管理员详情
  permission: admin:user:read
  response: AdminUserDTO

PUT /admin/api/v1/admin-users/{adminUserId}
  summary: 更新管理员信息
  permission: admin:user:write
  body: { email, phone, nickname, avatarUrl }
  response: AdminUserDTO

PUT /admin/api/v1/admin-users/{adminUserId}/status
  summary: 更改管理员状态
  permission: admin:user:write
  body: { status: "active"|"disabled" }
  constraints: 不可禁用自己，不可禁用最后一个 super_admin

PUT /admin/api/v1/admin-users/{adminUserId}/password
  summary: 重置管理员密码
  permission: admin:user:write (他人) | authenticated (自己)
  body: { newPassword, [currentPassword] }

PUT /admin/api/v1/admin-users/{adminUserId}/roles
  summary: 设置管理员角色
  permission: admin:role:write
  body: { roleIds: [1, 2, 3] }
```

#### 会员管理（代理 member-service）

```yaml
GET /admin/api/v1/members
  summary: 会员列表
  permission: member:user:read
  params: page, size, keyword, status, startDate, endDate
  response: PageResult<MemberDTO>

GET /admin/api/v1/members/{memberId}
  summary: 会员详情
  permission: member:user:read
  response: MemberDetailDTO { member, roles, devices, socialConnections }

PUT /admin/api/v1/members/{memberId}/status
  summary: 修改会员状态
  permission: member:user:write
  body: { status: "active"|"suspended", reason }

GET /admin/api/v1/members/{memberId}/roles
  summary: 会员角色列表
  permission: member:role:read
  response: List<RoleDTO>

PUT /admin/api/v1/members/{memberId}/roles
  summary: 设置会员角色
  permission: member:role:write
  body: { roleIds: [1, 2] }

GET /admin/api/v1/members/{memberId}/devices
  summary: 会员设备列表
  permission: member:user:read
  response: List<DeviceDTO>
```

#### 平台角色权限管理（代理 member-service）

```yaml
GET    /admin/api/v1/roles                         # 角色列表
POST   /admin/api/v1/roles                         # 创建角色
GET    /admin/api/v1/roles/{roleId}                # 角色详情（含权限）
PUT    /admin/api/v1/roles/{roleId}                # 更新角色
DELETE /admin/api/v1/roles/{roleId}                # 删除角色
PUT    /admin/api/v1/roles/{roleId}/permissions    # 设置角色权限

GET    /admin/api/v1/permissions                   # 权限列表
POST   /admin/api/v1/permissions                   # 创建权限
PUT    /admin/api/v1/permissions/{permissionId}    # 更新权限
DELETE /admin/api/v1/permissions/{permissionId}    # 删除权限
```

#### 审计日志

```yaml
GET /admin/api/v1/audit-logs
  summary: 审计日志列表
  permission: admin:audit:read
  params: page, size, adminUserId, operationType, appCode, resourceType, startDate, endDate
  response: PageResult<AuditLogDTO>

GET /admin/api/v1/audit-logs/{auditLogId}
  summary: 审计日志详情
  permission: admin:audit:read
  response: AuditLogDTO
```

### 5.3 内部接口（供 auth-service 调用）

```yaml
# InternalAdminFacade (Feign Interface in admin-facade)

GET /internal/admin/users/username/{username}
  summary: 按用户名查找管理员
  response: AdminUserDTO

POST /internal/admin/users/authenticate
  summary: 验证管理员密码
  body: { username, password }
  response: AuthenticatedAdminDTO { adminUserId, username, passwordValid }

GET /internal/admin/users/{adminUserId}/permissions
  summary: 获取管理员权限集合
  response: Set<String>
```

---

## 6. 审计日志实现方案

### 6.1 AOP 自动记录

使用自定义注解 `@Auditable` + AOP 方式自动记录操作日志：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    OperationType operation();
    String appCode();
    String resourceType();
}

// Controller 使用
@Auditable(operation = OperationType.MEMBER_BANNED, appCode = "member", resourceType = "member")
@PutMapping("/{memberId}/status")
public Result<Void> updateMemberStatus(@PathVariable Long memberId, ...) { ... }
```

### 6.2 AuditLogAspect 处理流程

```
Controller method invoked
    │
    ▼
AuditLogAspect @Around
    ├── 1. 从 SecurityContext 提取 adminUserId, username
    ├── 2. 从 HttpServletRequest 提取 method, path, body, ip, userAgent
    ├── 3. 对 requestBody 脱敏（移除 password 等敏感字段）
    ├── 4. 执行目标方法
    ├── 5. 记录结果（SUCCESS / FAILED + exception message）
    └── 6. 异步写入 admin_audit_log 表（不影响主流程性能）
```

### 6.3 脱敏策略

| 字段模式                            | 脱敏规则                          |
| ----------------------------------- | --------------------------------- |
| `*password*`, `*secret*`, `*token*` | 替换为 `***`                      |
| 手机号                              | 保留前3后4：`138****1234`         |
| 邮箱                                | 保留首字符和@后：`a***@gmail.com` |

---

## 7. 跨服务集成

### 7.1 admin-service → member-service

通过 `MemberServiceClient`（OpenFeign）调用 member-service 的内部接口：

| admin 操作   | 调用的 member-service 接口                         | 方法     |
| ------------ | -------------------------------------------------- | -------- |
| 会员列表     | `GET /internal/member/list`                        | ⚠ 需新增 |
| 会员详情     | `GET /internal/member/{memberId}`                  | 已有     |
| 封禁/解封    | `POST /internal/member/{memberId}/lock` / `unlock` | 已有     |
| 会员角色列表 | `GET /internal/member/{memberId}/roles`            | ⚠ 需新增 |
| 设置会员角色 | `POST /memberRole/assign`                          | 已有     |
| 角色 CRUD    | → member-service 内部接口                          | ⚠ 需新增 |
| 权限 CRUD    | → member-service 内部接口                          | ⚠ 需新增 |

**需要在 member-service 新增的内部接口：**

- `GET /internal/member/list` — 分页会员列表（支持搜索/过滤）
- `GET /internal/member/{memberId}/roles` — 获取会员角色列表
- `GET /internal/roles` — 角色列表
- `POST /internal/roles` — 创建角色
- `PUT /internal/roles/{roleId}` — 更新角色
- `DELETE /internal/roles/{roleId}` — 删除角色
- `PUT /internal/roles/{roleId}/permissions` — 设置角色权限
- `GET /internal/permissions` — 权限列表
- `POST /internal/permissions` — 创建权限
- `PUT /internal/permissions/{id}` — 更新权限
- `DELETE /internal/permissions/{id}` — 删除权限

### 7.2 auth-service → admin-service

auth-service 通过 `AdminServiceClient`（使用 admin-facade 定义的接口）调用 admin-service：

| 场景       | Facade 接口                        | 描述               |
| ---------- | ---------------------------------- | ------------------ |
| 管理员登录 | `findByUsername(username)`         | 查找管理员账号     |
| 密码验证   | `authenticate(username, password)` | 校验管理员密码     |
| 权限查询   | `getPermissions(adminUserId)`      | 获取管理员权限集合 |

### 7.3 事件通信

| 事件                          | 发布方        | 消费方       | 描述                              |
| ----------------------------- | ------------- | ------------ | --------------------------------- |
| `AdminPermissionChangedEvent` | admin-service | auth-service | 管理员权限变更，清除 JWT 权限缓存 |

---

## 8. 权限缓存方案

与 member-service 权限缓存一致：

```
Redis Key:   admin:perm:{adminUserId}
Value:       Set<String> (permission codes)
TTL:         30 minutes

Redis Key:   admin:perm:version:{adminUserId}
Value:       Long (permission version)
TTL:         30 minutes
```

权限变更时版本号 +1，Gateway 对比 JWT 中的 `perm_version` 与 Redis 中的最新版本，不一致时拒绝请求并提示刷新 Token。

---

## 9. 多应用扩展架构

### 9.1 设计原则

```
┌────────────────────────────────────────────────┐
│                admin-service                    │
│                                                │
│  ┌──────────────────────────────────────────┐  │
│  │        Application Management Layer       │  │
│  │  ┌─────────┐ ┌─────────┐ ┌──────────┐   │  │
│  │  │ member  │ │   im    │ │   mall   │   │  │
│  │  │ adapter │ │ adapter │ │ adapter  │   │  │
│  │  └────┬────┘ └────┬────┘ └────┬─────┘   │  │
│  └───────┼───────────┼──────────┼──────────┘  │
│          │           │          │              │
└──────────┼───────────┼──────────┼──────────────┘
           │           │          │
    ┌──────▼──┐  ┌─────▼───┐ ┌───▼──────┐
    │ member  │  │   im    │ │   mall   │
    │ service │  │ service │ │ service  │
    └─────────┘  └─────────┘ └──────────┘
```

### 9.2 扩展新应用的步骤

1. 在 `admin_application` 注册新应用（如 `mall`）
2. 在 `admin_permission` 添加应用级权限（如 `mall:product:read`、`mall:order:write`）
3. 实现新应用的 `XxxServiceClient`（Feign Client）
4. 实现新应用的 `XxxServiceAdapter`（适配 admin 管理操作）
5. 添加对应的 Controller 和 ApplicationService

权限码的 `{app}:` 前缀天然实现应用级隔离，无需修改已有框架代码。

---

## 10. 分期实施计划

### Phase 1（预计 3-4 周）

```
Week 1: 基础骨架
  ├── admin-facade: InternalAdminFacade, DTOs
  ├── admin-service: DDD 分层骨架
  ├── 数据库: admin_user, admin_role, admin_permission, admin_role_permission,
  │           admin_user_role, admin_audit_log
  ├── AdminUser 聚合 + AdminUserDomainService
  └── AdminUserRepository + Mapper

Week 2: 管理员账号 + RBAC
  ├── AdminUserApplicationService (CRUD + 状态管理)
  ├── AdminUserController + InternalAdminController
  ├── 管理员 RBAC 完整实现
  ├── 权限缓存（Redis）
  └── auth-service 集成: 实现 AdminModuleAdapter

Week 3: 会员管理 + 角色权限代理
  ├── member-service: 新增所需内部接口
  ├── MemberServiceClient (Feign)
  ├── MemberManagementController + ApplicationService
  ├── RoleManagementController + PermissionManagementController
  └── 会员列表、搜索、状态管理、角色管理

Week 4: 审计日志 + 联调测试
  ├── AuditLog 实现 (@Auditable + AOP)
  ├── AuditLogController
  ├── Gateway 路由配置
  ├── 端到端联调
  └── 单元测试 + 集成测试
```

### Phase 2（预计 2-3 周）

```
Week 5-6: 系统配置 + 公告
  ├── admin_system_config, admin_announcement, admin_application 表
  ├── SystemConfig 聚合 + CRUD
  ├── Announcement 聚合 + 状态流转
  ├── Application 注册管理

Week 7: 数据看板
  ├── member-service: 新增统计内部接口
  ├── im-service: 新增统计内部接口
  ├── DashboardApplicationService (聚合多源数据)
  ├── Redis 缓存统计数据
  └── DashboardController
```

### Phase 3（根据业务需要）

```
  ├── IM 内容管理
  ├── 多应用管理脚手架
  └── 高级数据分析
```

---

## 11. 技术风险与缓解

| 风险                                         | 影响 | 缓解措施                                           |
| -------------------------------------------- | ---- | -------------------------------------------------- |
| auth-service AdminModuleAdapter 改造涉及面广 | 中   | 复用现有 Strategy 模式，admin 仅需新增一条认证分支 |
| member-service 需新增大量内部接口            | 中   | 内部接口走 InternalController，不暴露给网关        |
| 审计日志高并发写入影响性能                   | 低   | 异步写入 + 批量刷盘；极端情况可接入 MQ 缓冲        |
| 多应用权限码膨胀                             | 低   | 按 app_code 分组管理，权限树 UI 支持折叠           |
| Feign 调用 member-service 超时/失败          | 中   | 配置合理超时 + 降级处理（返回受限信息而非报错）    |
