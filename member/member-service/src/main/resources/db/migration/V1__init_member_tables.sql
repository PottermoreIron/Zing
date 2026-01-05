-- V1: 初始化会员系统数据库表结构
-- 包含：用户表、设备表、社交连接表、角色表、权限表及关联表

-- 会员基础信息表
CREATE TABLE `member_member`
(
    `id`                    BIGINT UNSIGNED                                                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`        TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`        TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`        TIMESTAMP                                                        NULL     DEFAULT NULL COMMENT '软删除时间',

    -- 基本身份信息
    `member_id`             BIGINT UNSIGNED                                                  NOT NULL COMMENT '用户唯一标识符',
    `nickname`              VARCHAR(30)                                                      NOT NULL COMMENT '用户名',
    `email`                 VARCHAR(255)                                                     NULL COMMENT '邮箱地址',
    `phone`                 VARCHAR(20)                                                      NULL COMMENT '手机号码',
    `password_hash`         VARCHAR(255)                                                     NOT NULL COMMENT '密码哈希值',

    -- 个人信息
    `first_name`            VARCHAR(50)                                                      NULL COMMENT '名',
    `last_name`             VARCHAR(50)                                                      NULL COMMENT '姓',
    `gender`                TINYINT                                                                   DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `birth`                 DATE                                                             NULL COMMENT '出生日期',
    `avatar_url`            VARCHAR(500)                                                     NULL COMMENT '头像URL',

    -- 地理位置信息
    `country_code`          CHAR(2)                                                          NULL COMMENT 'ISO 3166-1 alpha-2 国家代码',
    `region`                VARCHAR(100)                                                     NULL COMMENT '省/州/地区',
    `city`                  VARCHAR(100)                                                     NULL COMMENT '城市',
    `timezone`              VARCHAR(30)                                                      NULL COMMENT '时区',
    `locale`                VARCHAR(10)                                                      NULL     DEFAULT 'en_US' COMMENT '语言区域设置',

    -- 账户状态
    `status`                ENUM ('active', 'inactive', 'suspended', 'pending_verification') NOT NULL DEFAULT 'pending_verification' COMMENT '账户状态',
    `gmt_email_verified_at` TIMESTAMP                                                        NULL COMMENT '邮箱验证时间',
    `gmt_phone_verified_at` TIMESTAMP                                                        NULL COMMENT '手机验证时间',
    `gmt_last_login_at`     TIMESTAMP                                                        NULL COMMENT '最后登录时间',
    `last_login_ip`         VARCHAR(45)                                                      NULL COMMENT '最后登录IP地址',

    -- 扩展字段
    `extend_json`           JSON                                                             NULL COMMENT '扩展元数据',

    PRIMARY KEY (`id`),

    -- 核心业务唯一索引
    UNIQUE KEY `uk_member_id` (`member_id`),
    UNIQUE KEY `uk_nickname` (`nickname`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),

    -- 登录认证相关索引
    KEY `idx_login_email_status` (`email`, `status`, `gmt_deleted_at`),
    KEY `idx_login_phone_status` (`phone`, `status`, `gmt_deleted_at`),

    -- 账户状态和管理索引
    KEY `idx_status_created` (`status`, `gmt_created_at`),
    KEY `idx_active_members` (`status`, `gmt_deleted_at`, `gmt_last_login_at`),

    -- 软删除和时间范围查询索引
    KEY `idx_deleted_at` (`gmt_deleted_at`),
    KEY `idx_created_range` (`gmt_created_at`, `gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='会员基础信息表';

-- 用户设备表
CREATE TABLE `member_device`
(
    `id`               BIGINT UNSIGNED                                           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`   TIMESTAMP                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`   TIMESTAMP                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`   TIMESTAMP                                                 NULL     DEFAULT NULL COMMENT '软删除时间',

    `member_id`        BIGINT UNSIGNED                                           NOT NULL COMMENT '用户ID',
    `device_id`        VARCHAR(128)                                              NOT NULL COMMENT '设备唯一标识',
    `device_type`      ENUM ('mobile', 'tablet', 'desktop', 'smart_tv', 'other') NOT NULL COMMENT '设备类型',
    `platform`         VARCHAR(30)                                               NULL COMMENT '操作系统平台',
    `browser`          VARCHAR(60)                                               NULL COMMENT '浏览器信息',
    `app_version`      VARCHAR(20)                                               NULL COMMENT '应用版本',
    `push_token`       VARCHAR(255)                                              NULL COMMENT '推送令牌',
    `is_active`        BOOLEAN                                                   NOT NULL DEFAULT TRUE COMMENT '是否活跃设备',
    `gmt_last_used_at` TIMESTAMP                                                 NULL COMMENT '最后使用时间',

    PRIMARY KEY (`id`),

    -- 核心业务索引
    UNIQUE KEY `uk_device_id` (`device_id`),

    -- 用户设备关联索引（高频查询）
    KEY `idx_member_devices` (`member_id`, `is_active`, `gmt_deleted_at`),
    KEY `idx_member_active_devices` (`member_id`, `is_active`, `gmt_last_used_at`),

    -- 设备管理和统计索引
    KEY `idx_device_type_analysis` (`device_type`, `platform`, `is_active`),
    KEY `idx_active_devices` (`is_active`, `gmt_last_used_at`, `gmt_deleted_at`),

    -- 设备使用情况分析索引
    KEY `idx_platform_stats` (`platform`, `device_type`, `gmt_created_at`),
    KEY `idx_last_used_cleanup` (`gmt_last_used_at`, `is_active`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户设备信息表';

-- 第三方平台连接表
CREATE TABLE `member_social_connections`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`       TIMESTAMP       NULL     DEFAULT NULL COMMENT '软删除时间',

    `member_id`            BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `provider`             VARCHAR(255)    NOT NULL COMMENT '第三方平台提供商',
    `provider_member_id`   VARCHAR(128)    NOT NULL COMMENT '第三方平台用户ID',
    `provider_username`    VARCHAR(100)    NULL COMMENT '第三方平台用户名',
    `provider_email`       VARCHAR(255)    NULL COMMENT '第三方平台邮箱',

    -- OAuth 令牌信息
    `access_token`         TEXT            NULL COMMENT '访问令牌',
    `refresh_token`        TEXT            NULL COMMENT '刷新令牌',
    `gmt_token_expires_at` TIMESTAMP       NULL COMMENT '令牌过期时间',
    `scope`                VARCHAR(500)    NULL COMMENT '授权范围',

    -- 连接状态
    `is_active`            BOOLEAN         NOT NULL DEFAULT TRUE COMMENT '连接是否活跃',
    `gmt_last_sync_at`     TIMESTAMP       NULL COMMENT '最后同步时间',

    -- 扩展信息
    `extend_json`          JSON            NULL COMMENT '第三方平台原始数据',

    PRIMARY KEY (`id`),

    -- 核心业务唯一索引
    UNIQUE KEY `uk_member_provider` (`member_id`, `provider`, `gmt_deleted_at`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_member_id`, `gmt_deleted_at`),

    -- 用户社交连接查询索引
    KEY `idx_member_connections` (`member_id`, `is_active`, `gmt_deleted_at`),

    -- 第三方平台管理索引
    KEY `idx_provider_active` (`provider`, `is_active`, `gmt_deleted_at`),
    KEY `idx_provider_stats` (`provider`, `gmt_created_at`),

    -- OAuth令牌管理索引
    KEY `idx_token_expiry` (`gmt_token_expires_at`, `is_active`),
    KEY `idx_sync_status` (`gmt_last_sync_at`, `is_active`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户第三方平台连接表';

-- 角色表
CREATE TABLE `member_role`
(
    `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`   TIMESTAMP        NULL     DEFAULT NULL COMMENT '软删除时间',

    `role_code`        VARCHAR(50)      NOT NULL COMMENT '角色编码，如：admin、editor、viewer',
    `role_name`        VARCHAR(100)     NOT NULL COMMENT '角色名称',
    `role_description` TEXT             NULL COMMENT '角色描述',
    `role_level`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色级别，数字越大权限越高',
    `is_system_role`   BOOLEAN          NOT NULL DEFAULT FALSE COMMENT '是否为系统内置角色',
    `is_active`        BOOLEAN          NOT NULL DEFAULT TRUE COMMENT '是否启用',

    -- 扩展字段
    `extend_json`      JSON             NULL COMMENT '角色扩展配置',

    PRIMARY KEY (`id`),

    -- 核心业务索引
    UNIQUE KEY `uk_role_code` (`role_code`, `gmt_deleted_at`),

    -- 角色管理和查询索引
    KEY `idx_active_roles` (`is_active`, `gmt_deleted_at`, `role_level`),
    KEY `idx_system_roles` (`is_system_role`, `is_active`, `gmt_deleted_at`),
    KEY `idx_role_hierarchy` (`role_level`, `is_active`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色表';

-- 权限表
CREATE TABLE `member_permission`
(
    `id`                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`         TIMESTAMP        NULL     DEFAULT NULL COMMENT '软删除时间',

    `permission_code`        VARCHAR(100)     NOT NULL COMMENT '权限编码，如：user.create、content.edit',
    `permission_name`        VARCHAR(100)     NOT NULL COMMENT '权限名称',
    `permission_description` TEXT             NULL COMMENT '权限描述',
    `resource_type`          VARCHAR(50)      NOT NULL COMMENT '资源类型，如：user、content、system',
    `action_type`            VARCHAR(50)      NOT NULL COMMENT '操作类型，如：create、read、update、delete',
    `parent_id`              BIGINT UNSIGNED  NULL COMMENT '父权限ID，支持权限分组',
    `permission_level`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '权限层级',
    `is_system_permission`   BOOLEAN          NOT NULL DEFAULT FALSE COMMENT '是否为系统内置权限',
    `is_active`              BOOLEAN          NOT NULL DEFAULT TRUE COMMENT '是否启用',

    -- 扩展字段
    `extend_json`            JSON             NULL COMMENT '权限扩展配置',

    PRIMARY KEY (`id`),

    -- 核心业务索引
    UNIQUE KEY `uk_permission_code` (`permission_code`, `gmt_deleted_at`),

    -- 权限查询和管理索引
    KEY `idx_resource_action` (`resource_type`, `action_type`, `is_active`),
    KEY `idx_active_permissions` (`is_active`, `gmt_deleted_at`, `permission_level`),
    KEY `idx_system_permissions` (`is_system_permission`, `is_active`, `gmt_deleted_at`),

    -- 权限层级和树形结构索引
    KEY `idx_permission_tree` (`parent_id`, `permission_level`, `is_active`),
    KEY `idx_permission_hierarchy` (`permission_level`, `resource_type`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='权限表';

-- 角色权限关联表
CREATE TABLE `member_role_permission`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at` TIMESTAMP       NULL     DEFAULT NULL COMMENT '软删除时间',

    `role_id`        BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id`  BIGINT UNSIGNED NOT NULL COMMENT '权限ID',

    PRIMARY KEY (`id`),

    -- 核心关联索引
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`, `gmt_deleted_at`),

    -- 权限检查和查询索引
    KEY `idx_role_permissions` (`role_id`, `gmt_deleted_at`),
    KEY `idx_permission_roles` (`permission_id`, `gmt_deleted_at`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色权限关联表';

-- 用户角色关联表
CREATE TABLE `member_member_role`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gmt_created_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_updated_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `gmt_deleted_at`  TIMESTAMP       NULL     DEFAULT NULL COMMENT '软删除时间',

    `member_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id`         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `gmt_expires_at`  TIMESTAMP       NULL COMMENT '过期时间，NULL表示永久有效',
    `is_active`       BOOLEAN         NOT NULL DEFAULT TRUE COMMENT '是否启用',

    -- 扩展字段
    `assignment_note` VARCHAR(500)    NULL COMMENT '分配备注',
    `extend_json`     JSON            NULL COMMENT '分配扩展信息',

    PRIMARY KEY (`id`),

    -- 核心关联索引
    UNIQUE KEY `uk_member_role` (`member_id`, `role_id`, `gmt_deleted_at`),

    -- 用户权限检查索引（最高频使用）
    KEY `idx_member_active_roles` (`member_id`, `is_active`, `gmt_deleted_at`, `gmt_expires_at`),

    -- 角色成员管理索引
    KEY `idx_role_members` (`role_id`, `is_active`, `gmt_deleted_at`),

    -- 过期角色清理索引
    KEY `idx_expired_roles` (`gmt_expires_at`, `is_active`),
    KEY `idx_role_expiry_check` (`is_active`, `gmt_expires_at`, `gmt_deleted_at`),

    -- 角色分配审计索引
    KEY `idx_assignment_audit` (`gmt_created_at`, `member_id`, `role_id`),

    -- 软删除索引
    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户角色关联表';
