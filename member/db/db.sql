CREATE TABLE `member_member`
(
    `id`                    BIGINT UNSIGNED                                                  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`        TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`        TIMESTAMP                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`        TIMESTAMP                                                        NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `member_id`             BIGINT UNSIGNED                                                  NOT NULL COMMENT 'Unique member identifier',
    `nickname`              VARCHAR(30)                                                      NOT NULL COMMENT 'Nickname / display name',
    `email`                 VARCHAR(255)                                                     NULL COMMENT 'Email address',
    `phone`                 VARCHAR(20)                                                      NULL COMMENT 'Phone number',
    `password_hash`         VARCHAR(255)                                                     NOT NULL COMMENT 'BCrypt password hash',

    `first_name`            VARCHAR(50)                                                      NULL COMMENT 'First name',
    `last_name`             VARCHAR(50)                                                      NULL COMMENT 'Last name',
    `gender`                TINYINT                                                                   DEFAULT 0 COMMENT 'Gender: 0=Unknown, 1=Male, 2=Female',
    `birth`                 DATE                                                             NULL COMMENT 'Date of birth',
    `avatar_url`            VARCHAR(500)                                                     NULL COMMENT 'Avatar URL',

    `country_code`          CHAR(2)                                                          NULL COMMENT 'ISO 3166-1 alpha-2 country code',
    `region`                VARCHAR(100)                                                     NULL COMMENT 'Province / state / region',
    `city`                  VARCHAR(100)                                                     NULL COMMENT 'City',
    `timezone`              VARCHAR(30)                                                      NULL COMMENT 'Timezone (IANA format)',
    `locale`                VARCHAR(10)                                                      NULL     DEFAULT 'en_US' COMMENT 'Locale (BCP 47 tag)',

    `status`                ENUM ('active', 'inactive', 'suspended', 'pending_verification') NOT NULL DEFAULT 'pending_verification' COMMENT 'Account status',
    `gmt_email_verified_at` TIMESTAMP                                                        NULL COMMENT 'Email verification timestamp',
    `gmt_phone_verified_at` TIMESTAMP                                                        NULL COMMENT 'Phone verification timestamp',
    `gmt_last_login_at`     TIMESTAMP                                                        NULL COMMENT 'Last login timestamp',
    `last_login_ip`         VARCHAR(45)                                                      NULL COMMENT 'Last login IP address',

    `extend_json`           JSON                                                             NULL COMMENT 'Additional extension metadata (JSON)',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_member_id` (`member_id`),
    UNIQUE KEY `uk_nickname` (`nickname`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),

    KEY `idx_login_email_status` (`email`, `status`, `gmt_deleted_at`),
    KEY `idx_login_phone_status` (`phone`, `status`, `gmt_deleted_at`),

    KEY `idx_status_created` (`status`, `gmt_created_at`),
    KEY `idx_active_members` (`status`, `gmt_deleted_at`, `gmt_last_login_at`),

    KEY `idx_deleted_at` (`gmt_deleted_at`),
    KEY `idx_created_range` (`gmt_created_at`, `gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Member profile table';

CREATE TABLE `member_device`
(
    `id`               BIGINT UNSIGNED                                           NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`   TIMESTAMP                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`   TIMESTAMP                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`   TIMESTAMP                                                 NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `member_id`        BIGINT UNSIGNED                                           NOT NULL COMMENT 'Member ID reference',
    `device_id`        VARCHAR(128)                                              NOT NULL COMMENT 'Unique device identifier',
    `device_type`      ENUM ('mobile', 'tablet', 'desktop', 'smart_tv', 'other') NOT NULL COMMENT 'Device type',
    `platform`         VARCHAR(30)                                               NULL COMMENT 'OS platform',
    `browser`          VARCHAR(60)                                               NULL COMMENT 'Browser info',
    `app_version`      VARCHAR(20)                                               NULL COMMENT 'Application version',
    `push_token`       VARCHAR(255)                                              NULL COMMENT 'Push notification token',
    `is_active`        BOOLEAN                                                   NOT NULL DEFAULT TRUE COMMENT 'Whether this is the active device',
    `gmt_last_used_at` TIMESTAMP                                                 NULL COMMENT 'Last used timestamp',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_device_id` (`device_id`),

    KEY `idx_member_devices` (`member_id`, `is_active`, `gmt_deleted_at`),
    KEY `idx_member_active_devices` (`member_id`, `is_active`, `gmt_last_used_at`),

    KEY `idx_device_type_analysis` (`device_type`, `platform`, `is_active`),
    KEY `idx_active_devices` (`is_active`, `gmt_last_used_at`, `gmt_deleted_at`),

    KEY `idx_platform_stats` (`platform`, `device_type`, `gmt_created_at`),
    KEY `idx_last_used_cleanup` (`gmt_last_used_at`, `is_active`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Member device table';

CREATE TABLE `member_social_connection`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`       TIMESTAMP       NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `member_id`            BIGINT UNSIGNED NOT NULL COMMENT 'Member ID reference',
    `provider`             VARCHAR(255)    NOT NULL COMMENT 'OAuth2 provider name',
    `provider_member_id`   VARCHAR(128)    NOT NULL COMMENT 'User ID on the provider platform',
    `provider_username`    VARCHAR(100)    NULL COMMENT 'Username on the provider platform',
    `provider_email`       VARCHAR(255)    NULL COMMENT 'Email on the provider platform',

    `access_token`         TEXT            NULL COMMENT 'OAuth2 access token',
    `refresh_token`        TEXT            NULL COMMENT 'OAuth2 refresh token',
    `gmt_token_expires_at` TIMESTAMP       NULL COMMENT 'Token expiration timestamp',
    `scope`                VARCHAR(500)    NULL COMMENT 'OAuth2 granted scopes',

    `is_active`            BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Whether the connection is active',
    `gmt_last_sync_at`     TIMESTAMP       NULL COMMENT 'Last sync timestamp',

    `extend_json`          JSON            NULL COMMENT 'Raw provider data (JSON)',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_member_provider` (`member_id`, `provider`, `gmt_deleted_at`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_member_id`, `gmt_deleted_at`),

    KEY `idx_member_connections` (`member_id`, `is_active`, `gmt_deleted_at`),

    KEY `idx_provider_active` (`provider`, `is_active`, `gmt_deleted_at`),
    KEY `idx_provider_stats` (`provider`, `gmt_created_at`),

    KEY `idx_token_expiry` (`gmt_token_expires_at`, `is_active`),
    KEY `idx_sync_status` (`gmt_last_sync_at`, `is_active`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Member social connection table';

CREATE TABLE `member_role`
(
    `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`   TIMESTAMP        NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `role_code`        VARCHAR(50)      NOT NULL COMMENT 'Role code, e.g. admin, editor, viewer',
    `role_name`        VARCHAR(100)     NOT NULL COMMENT 'Role display name',
    `role_description` TEXT             NULL COMMENT 'Role description',
    `role_level`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Role level; higher number means higher privilege',
    `is_system_role`   BOOLEAN          NOT NULL DEFAULT FALSE COMMENT 'Whether this is a system built-in role',
    `is_active`        BOOLEAN          NOT NULL DEFAULT TRUE COMMENT 'Whether this record is active',

    `extend_json`      JSON             NULL COMMENT 'Role extension configuration (JSON)',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_role_code` (`role_code`, `gmt_deleted_at`),

    KEY `idx_active_roles` (`is_active`, `gmt_deleted_at`, `role_level`),
    KEY `idx_system_roles` (`is_system_role`, `is_active`, `gmt_deleted_at`),
    KEY `idx_role_hierarchy` (`role_level`, `is_active`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Role table';

CREATE TABLE `member_permission`
(
    `id`                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`         TIMESTAMP        NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `permission_code`        VARCHAR(100)     NOT NULL COMMENT 'Permission code, e.g. user.create, content.edit',
    `permission_name`        VARCHAR(100)     NOT NULL COMMENT 'Permission display name',
    `permission_description` TEXT             NULL COMMENT 'Permission description',
    `resource_type`          VARCHAR(50)      NOT NULL COMMENT 'Resource type, e.g. user, content, system',
    `action_type`            VARCHAR(50)      NOT NULL COMMENT 'Action type, e.g. create, read, update, delete',
    `parent_id`              BIGINT UNSIGNED  NULL COMMENT 'Parent permission ID for grouping',
    `permission_level`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Permission hierarchy level',
    `is_system_permission`   BOOLEAN          NOT NULL DEFAULT FALSE COMMENT 'Whether this is a system built-in permission',
    `is_active`              BOOLEAN          NOT NULL DEFAULT TRUE COMMENT 'Whether this record is active',

    `extend_json`            JSON             NULL COMMENT 'Permission extension configuration (JSON)',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_permission_code` (`permission_code`, `gmt_deleted_at`),

    KEY `idx_resource_action` (`resource_type`, `action_type`, `is_active`),
    KEY `idx_active_permissions` (`is_active`, `gmt_deleted_at`, `permission_level`),
    KEY `idx_system_permissions` (`is_system_permission`, `is_active`, `gmt_deleted_at`),

    KEY `idx_permission_tree` (`parent_id`, `permission_level`, `is_active`),
    KEY `idx_permission_hierarchy` (`permission_level`, `resource_type`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Permission table';

CREATE TABLE `member_role_permission`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at` TIMESTAMP       NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `role_id`        BIGINT UNSIGNED NOT NULL COMMENT 'Role ID reference',
    `permission_id`  BIGINT UNSIGNED NOT NULL COMMENT 'Permission ID reference',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`, `gmt_deleted_at`),

    KEY `idx_role_permissions` (`role_id`, `gmt_deleted_at`),
    KEY `idx_permission_roles` (`permission_id`, `gmt_deleted_at`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Role-permission association table';

CREATE TABLE `member_member_role`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `gmt_created_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `gmt_updated_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `gmt_deleted_at`  TIMESTAMP       NULL     DEFAULT NULL COMMENT 'Soft-delete timestamp',

    `member_id`       BIGINT UNSIGNED NOT NULL COMMENT 'Member ID reference',
    `role_id`         BIGINT UNSIGNED NOT NULL COMMENT 'Role ID reference',
    `gmt_expires_at`  TIMESTAMP       NULL COMMENT 'Expiration timestamp; NULL means permanent',
    `is_active`       BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Whether this record is active',

    `assignment_note` VARCHAR(500)    NULL COMMENT 'Assignment note',
    `extend_json`     JSON            NULL COMMENT 'Assignment extension data (JSON)',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_member_role` (`member_id`, `role_id`, `gmt_deleted_at`),

    KEY `idx_member_active_roles` (`member_id`, `is_active`, `gmt_deleted_at`, `gmt_expires_at`),

    KEY `idx_role_members` (`role_id`, `is_active`, `gmt_deleted_at`),

    KEY `idx_expired_roles` (`gmt_expires_at`, `is_active`),
    KEY `idx_role_expiry_check` (`is_active`, `gmt_expires_at`, `gmt_deleted_at`),

    KEY `idx_assignment_audit` (`gmt_created_at`, `member_id`, `role_id`),

    KEY `idx_deleted_at` (`gmt_deleted_at`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Member-role association table';