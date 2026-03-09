# Zing 项目架构分析与开发计划

> 生成日期：2026-03-09

---

## 一、系统架构概览

### 1.1 整体技术栈

| 分类       | 技术                       | 版本        |
| ---------- | -------------------------- | ----------- |
| 语言       | Java                       | 21          |
| 框架       | Spring Boot                | 3.4.2       |
| 微服务     | Spring Cloud               | 2024.0.2    |
| 服务注册   | Spring Cloud Alibaba Nacos | 2023.0.3.3  |
| ORM        | MyBatis-Plus               | 3.5.12      |
| 数据库     | MySQL                      | 9.2.0       |
| 缓存       | Redis                      | -           |
| 网络       | Netty                      | 4.2.3.Final |
| JWT        | JJWT                       | 0.12.6      |
| 分布式ID   | Meituan Leaf               | 1.0.1       |
| 消息队列   | RabbitMQ / Kafka（双支持） | -           |
| API文档    | SpringDoc OpenAPI          | 2.8.9       |
| 第三方登录 | weixin-java-mp（微信）     | 4.7.7.B     |

### 1.2 模块拓扑

```
zing (父 pom)
├── dependencies          # 版本管理 BOM（统一依赖版本）
├── framework             # 框架基础层（自研 Starter）
│   ├── framework-common              # 公共工具（R模型、异常、工具类）
│   ├── framework-starter-id          # 分布式ID（Meituan Leaf 封装）
│   ├── framework-starter-redis       # Redis 服务封装
│   ├── framework-starter-ratelimit   # 限流（Guava/Redis 双实现）
│   ├── framework-starter-mq          # 消息队列抽象（Kafka/RabbitMQ 双适配）
│   ├── framework-starter-touch       # 触达服务（SMS/Email 多渠道）
│   └── framework-starter-code-generator  # 代码生成器
├── gateway               # API 网关（Spring Cloud Gateway）
├── auth                  # 认证授权服务（DDD + 六边形架构）
│   ├── auth-facade       # API 接口定义
│   └── auth-service      # 核心实现
├── member                # 会员服务（DDD）
│   ├── member-facade     # API 接口定义
│   └── member-service    # 核心实现
├── im                    # 即时通讯服务（Netty TCP + REST）
│   ├── im-facade         # API 接口定义
│   └── im-service        # 核心实现
└── admin                 # 管理后台服务（待开发）
    ├── admin-facade
    └── admin-service
```

### 1.3 请求流转路径

```
Client
  │
  ▼
Gateway（JWT验证 + 权限版本校验 + Header注入）
  │
  ├──▶ auth-service（登录/注册/Token刷新/验证码）
  │       └── 通过 MemberServiceClient (Feign) 调用 member-service
  │
  ├──▶ member-service（会员信息/RBAC权限/设备/社交账号）
  │       └── 权限变更时通过 RabbitMQ/Kafka 发布事件到 auth-service
  │
  ├──▶ im-service（Netty TCP长连接 + REST HTTP接口）
  │
  └──▶ admin-service（管理后台，待开发）
```

### 1.4 核心设计模式

| 模块                        | 设计模式                                                                                          |
| --------------------------- | ------------------------------------------------------------------------------------------------- |
| auth-service                | DDD（领域驱动）+ 六边形架构（Ports & Adapters）+ Strategy（登录/注册/认证策略）+ Validation Chain |
| member-service              | DDD（聚合根 MemberAggregate/RoleAggregate/PermissionAggregate）+ Repository 模式                  |
| im-service                  | Netty Pipeline + MessageProcessor 工厂模式 + ConnectionManager                                    |
| framework-starter-ratelimit | AOP + 策略模式（IP/用户/固定 限流键 + Guava/Redis 限流实现）                                      |
| framework-starter-touch     | 模板方法 + 策略模式（渠道选择 + 降级）                                                            |
| framework-starter-mq        | 适配器模式（统一 MessageTemplate API，屏蔽 Kafka/RabbitMQ 差异）                                  |

---

## 二、各功能模块完成度评分

### 2.1 Framework 框架层 ⭐⭐⭐⭐☆ (8/10)

| 子模块                           | 完成状态    | 说明                                                               |
| -------------------------------- | ----------- | ------------------------------------------------------------------ |
| framework-common                 | ✅ 完成     | R模型、全局异常Handler、常用工具类完整                             |
| framework-starter-id             | ✅ 完成     | Leaf 分布式ID封装完整，含异常处理                                  |
| framework-starter-redis          | ✅ 完成     | Redis服务封装完整                                                  |
| framework-starter-ratelimit      | ✅ 完成     | AOP注解限流，支持IP/用户/固定三种key，Guava/Redis双实现            |
| framework-starter-mq             | ⚠️ 基本完成 | 生产者完整（Kafka/RabbitMQ），消费者注册机制存在但无实际消费者实现 |
| framework-starter-touch          | ⚠️ 基本完成 | 抽象层完整，多渠道降级，**缺实际 SMS/Email provider实现**          |
| framework-starter-code-generator | ✅ 完成     | 代码生成逻辑完整                                                   |

**扣分项：**

- touch 模块无实际短信服务商（阿里云/腾讯云）和邮件服务商（SMTP/SendGrid）实现
- MQ 消费者注册机制设计完整但无具体业务消费者

---

### 2.2 Gateway 网关 ⭐⭐⭐☆☆ (6/10)

| 功能                 | 完成状态 | 说明                                             |
| -------------------- | -------- | ------------------------------------------------ |
| JWT Token 验证       | ✅ 完成  | RSA公钥验证、异常处理完整                        |
| 权限版本号校验       | ✅ 完成  | Redis读取当前版本，与Token中版本比对             |
| 白名单路径           | ✅ 完成  | `/auth/login`、`/auth/register`、`/auth/refresh` |
| 用户信息 Header 注入 | ✅ 完成  | `X-User-Id`、`X-Perm-Version`、`X-Perm-Digest`   |
| 路由配置             | ❌ 未见  | 无 `application.yml` 路由规则（需补充）          |
| 服务发现集成         | ❌ 未见  | Nacos服务发现集成未验证                          |
| 熔断降级             | ❌ 缺失  | 无 Resilience4j / Sentinel 配置                  |
| 请求日志链路追踪     | ❌ 缺失  | 无 TraceId 注入                                  |

---

### 2.3 Auth 认证授权服务 ⭐⭐⭐⭐☆ (7.5/10)

| 功能                    | 完成状态  | 说明                                                          |
| ----------------------- | --------- | ------------------------------------------------------------- |
| 传统登录（4种方式）     | ✅ 完成   | 用户名密码、邮箱密码、邮箱验证码、手机验证码                  |
| 传统注册（4种方式）     | ✅ 完成   | 对应4种注册方式                                               |
| 一键认证（一站式）      | ✅ 完成   | 自动判断注册/登录，支持7种方式含OAuth2/微信                   |
| JWT Token 生成          | ✅ 完成   | 含 `perm_version` + `perm_digest` 字段                        |
| Token 刷新              | ✅ 完成   | RefreshToken机制                                              |
| 验证码发送              | ✅ 完成   | 邮件/短信双渠道，含限流                                       |
| 接口限流                | ✅ 完成   | 所有认证端点均有 IP 限流保护                                  |
| 权限缓存 & 版本管理     | ✅ 完成   | Redis缓存权限集合、版本号递增、摘要计算                       |
| 权限变更事件监听        | ✅ 完成   | MQ消费 member-service 权限变更，失效缓存                      |
| 权限校验注解            | ✅ 完成   | `@RequirePermission`、`@RequireRole`、`@RequireAnyPermission` |
| OAuth2 登录实现         | ⚠️ 接口   | Port 定义完整，**无实际 HTTP 调用实现**                       |
| 微信登录实现            | ⚠️ 接口   | Port 定义完整，**无实际微信接口对接**                         |
| 退出登录 / Token 黑名单 | ❌ 缺失   | 无 logout 端点，Token 无法主动失效                            |
| 多设备会话管理          | ❌ 缺失   | 无会话数量限制、踢下线逻辑                                    |
| Admin 域用户认证        | ⚠️ 桩代码 | `AdminModuleAdapter` 存在但未实现                             |

---

### 2.4 Member 会员服务 ⭐⭐⭐☆☆ (6/10)

| 功能                   | 完成状态  | 说明                                                               |
| ---------------------- | --------- | ------------------------------------------------------------------ |
| 会员 DDD 聚合根        | ✅ 完成   | `MemberAggregate` 含完整领域方法                                   |
| 会员基础 CRUD          | ✅ 完成   | 通过 `MemberApplicationService` + Repository                       |
| RBAC 角色权限管理      | ✅ 完成   | Role/Permission/MemberRole CRUD + 分配/撤销                        |
| 权限变更事件发布       | ✅ 完成   | 角色/权限变更后通过 MQ 发布事件                                    |
| 社交账号绑定           | ✅ 完成   | `SocialConnection` + Facade 接口                                   |
| 设备管理               | ✅ 完成   | Device CRUD                                                        |
| 内部认证接口           | ✅ 完成   | `InternalAuthController`、`InternalPermissionController`           |
| 基础 Service 实现      | ⚠️ 空壳   | `MemberServiceImpl` 等多数仅继承 `ServiceImpl`，**无业务逻辑重写** |
| 会员列表 / 搜索 / 分页 | ❌ 缺失   | 无会员列表查询接口                                                 |
| 头像上传               | ❌ 缺失   | 无 OSS 集成                                                        |
| 邮箱/手机号验证流程    | ❌ 缺失   | 注册后无验证流程触发                                               |
| 会员积分/等级          | ❌ 未规划 | 扩展功能                                                           |

---

### 2.5 IM 即时通讯服务 ⭐⭐☆☆☆ (4/10)

| 功能                  | 完成状态    | 说明                                          |
| --------------------- | ----------- | --------------------------------------------- |
| Netty TCP 服务器      | ✅ 完成     | IMServer 启动、优雅关闭                       |
| 连接管理              | ✅ 完成     | ConnectionManager（用户-Channel 双向映射）    |
| 自定义二进制协议      | ✅ 完成     | ProtocolEncoder/Decoder、ProtocolHeader       |
| 消息处理器工厂        | ✅ 完成     | 支持多处理器、优先级排序、异步执行            |
| 鉴权处理器            | ✅ 完成     | `AuthRequestProcessor`                        |
| 心跳处理器            | ✅ 完成     | `HeartbeatProcessor`                          |
| 数据库实体 & Mapper   | ✅ 完成     | Message、Conversation、Friend、Group等10个表  |
| **业务 Service 实现** | ❌ **全空** | 所有 ServiceImpl 均为空，仅继承 `ServiceImpl` |
| **REST 接口实现**     | ❌ **全空** | 所有 Controller 均为空                        |
| 消息路由 / 投递逻辑   | ❌ 缺失     | 无实际消息发送逻辑                            |
| 离线消息存储          | ❌ 缺失     | 无离线消息队列                                |
| 消息已读回执          | ❌ 缺失     | 有 `MessageReadStatus` 表但无逻辑             |
| 推送通知集成          | ❌ 缺失     | 无 APNs/FCM 集成                              |
| 分布式部署支持        | ❌ 缺失     | ConnectionManager 纯内存，无 Redis 集群路由   |
| 群组聊天逻辑          | ❌ 缺失     | 有实体无实现                                  |
| 文件传输              | ❌ 缺失     | 有 File 实体无实现                            |

---

### 2.6 Admin 管理后台 ⭐☆☆☆☆ (1/10)

| 功能     | 完成状态        | 说明                        |
| -------- | --------------- | --------------------------- |
| 项目骨架 | ✅ 存在         | pom.xml、启动类、facade定义 |
| 管理功能 | ❌ **全部缺失** | 无任何业务实现              |

---

## 三、总体完成度汇总

| 模块          | 得分   | 权重 | 加权得分       |
| ------------- | ------ | ---- | -------------- |
| Framework     | 8/10   | 15%  | 1.2            |
| Gateway       | 6/10   | 10%  | 0.6            |
| Auth 认证授权 | 7.5/10 | 25%  | 1.875          |
| Member 会员   | 6/10   | 20%  | 1.2            |
| IM 即时通讯   | 4/10   | 20%  | 0.8            |
| Admin 后台    | 1/10   | 10%  | 0.1            |
| **综合评分**  |        |      | **5.775 / 10** |

> **总结：** 项目架构设计清晰，框架层和认证层质量较高，但 IM 服务和 Admin 服务尚处于骨架阶段，大量业务逻辑待实现。

---

## 四、TODO 优先级列表

### 🔴 P0 — 核心缺失，立即补全

| #   | 任务                                  | 模块           | 说明                                                                                                                |
| --- | ------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------- |
| 1   | **实现 IM Service 所有业务逻辑**      | im-service     | FriendServiceImpl、MessageServiceImpl、GroupServiceImpl、ConversationServiceImpl 等10个均为空壳，需实现完整业务逻辑 |
| 2   | **实现 IM 所有 REST Controller**      | im-service     | MessageController、FriendController、GroupController 等10个均为空，需完整实现 CRUD                                  |
| 3   | **实现 IM 消息投递路由**              | im-service     | 核心功能：收到消息后查询目标用户 Channel，推送消息；目标离线则存入离线队列                                          |
| 4   | **实现 Auth 退出登录 / Token 黑名单** | auth-service   | 添加 `/auth/api/v1/logout` 接口，将 Token JTI 加入 Redis 黑名单，Gateway 过滤时检查                                 |
| 5   | **修复 Member Service 空实现**        | member-service | 将基础 ServiceImpl 补充业务逻辑，或明确哪些由 ApplicationService 直接代理                                           |

---

### 🟠 P1 — 重要功能，尽快完成

| #   | 任务                         | 模块                    | 说明                                                                  |
| --- | ---------------------------- | ----------------------- | --------------------------------------------------------------------- |
| 6   | **实现 Touch 短信 Provider** | framework-starter-touch | 接入阿里云短信 / 腾讯云短信，实现 `AbstractSmsChannelImpl` 子类       |
| 7   | **实现 Touch 邮件 Provider** | framework-starter-touch | 接入 SMTP / SendGrid，实现 `AbstractEmailChannelImpl` 子类            |
| 8   | **实现 OAuth2 登录适配器**   | auth-service            | 实现 `OAuth2Port`，完成 Google/GitHub 等授权码换 Token 及用户信息获取 |
| 9   | **实现微信登录适配器**       | auth-service            | 完成 `WeChatPort`，通过 weixin-java-mp 换取 openid/unionid            |
| 10  | **补全 Gateway 路由配置**    | gateway                 | 编写 `application.yml` 服务路由规则，集成 Nacos 服务发现              |
| 11  | **IM 离线消息支持**          | im-service              | 用户离线时消息存入 Redis 队列或 DB，上线后推送                        |
| 12  | **Admin 服务基础功能**       | admin-service           | 实现会员管理（列表/封禁/解封）、角色权限分配、系统配置 CRUD           |

---

### 🟡 P2 — 重要提升，计划完成

| #   | 任务                       | 模块                        | 说明                                                                |
| --- | -------------------------- | --------------------------- | ------------------------------------------------------------------- |
| 13  | **IM 分布式连接路由**      | im-service                  | 用 Redis 存储 userId → 服务节点 映射，实现跨节点消息路由            |
| 14  | **多设备会话管理**         | auth-service                | Token 中嵌入设备ID，支持踢下线、会话数量限制                        |
| 15  | **Member 列表查询 / 搜索** | member-service              | 分页查询会员列表，支持按状态/注册时间筛选                           |
| 16  | **头像 / 文件上传（OSS）** | member-service / im-service | 接入对象存储（阿里云 OSS / MinIO），实现头像上传和 IM 文件传输      |
| 17  | **Gateway 熔断降级**       | gateway                     | 集成 Resilience4j，对下游服务配置熔断器和限流                       |
| 18  | **Gateway 请求追踪**       | gateway                     | 注入 `X-Trace-Id`，配合日志实现全链路追踪                           |
| 19  | **邮箱/手机验证流程**      | member-service              | 注册完成后发送验证邮件/短信，确认后更新 `gmt_email_verified_at`     |
| 20  | **MQ 消费者实现**          | framework-starter-mq        | 实现 `MessageConsumerRegistry` 实际消费逻辑，确保权限事件被正确消费 |

---

### 🟢 P3 — 质量提升，持续迭代

| #   | 任务                    | 模块               | 说明                                                     |
| --- | ----------------------- | ------------------ | -------------------------------------------------------- |
| 21  | **单元测试补全**        | 全局               | 当前测试类均为空，至少补全 auth、member 核心逻辑测试     |
| 22  | **集成测试**            | auth / member      | 使用 Testcontainers 进行 MySQL/Redis 集成测试            |
| 23  | **OpenAPI 文档完善**    | auth / member / im | 补充 Controller 方法的 Swagger 注解                      |
| 24  | **Docker Compose 环境** | 全局               | 提供开发环境一键启动（MySQL、Redis、Nacos、RabbitMQ）    |
| 25  | **Flyway 数据库迁移**   | member / im        | 管理 DB schema 版本，避免手动执行 SQL                    |
| 26  | **Actuator / 监控接入** | 全局               | 配置健康检查、Prometheus metrics 端点                    |
| 27  | **IM 消息已读回执**     | im-service         | 实现 `MessageReadStatus` 完整逻辑，支持已读/未读状态同步 |
| 28  | **推送通知集成**        | im-service         | 离线用户通过 APNs/FCM 推送消息提醒                       |
| 29  | **Admin 运营功能**      | admin-service      | 数据看板、用户行为分析、系统公告等                       |

---

## 五、建议开发顺序（冲刺计划）

### Sprint 1（当前冲刺 —— 核心功能闭环）

```
Week 1-2:
  [P0] Touch 短信/邮件 Provider 实现     → 解除 auth 验证码发送阻塞
  [P0] Auth 退出/Token黑名单              → 安全基线达标
  [P1] OAuth2 + 微信登录适配器            → 社交登录闭环

Week 3-4:
  [P0] IM Service 业务逻辑实现（单聊为主）→ 核心 IM 功能可用
  [P0] IM REST Controller 实现           → REST API 可用
  [P0] IM 消息路由 + 离线消息基础版       → 基本可联调
```

### Sprint 2（功能完善）

```
Week 5-6:
  [P1] Gateway 路由 + Nacos 集成         → 微服务整体连通
  [P1] Admin 服务基础功能                 → 管理能力建立
  [P2] Member 列表 / 搜索                 → 运营基础

Week 7-8:
  [P2] OSS 文件上传                       → 头像 + IM 文件
  [P2] IM 分布式连接路由                  → 水平扩展能力
  [P2] 多设备会话管理                     → 安全体验提升
```

### Sprint 3（质量保障）

```
Week 9-10:
  [P3] 单元测试 + 集成测试                → 质量底线
  [P3] Docker Compose 开发环境            → 开发效率
  [P3] OpenAPI 文档                       → 协作效率
  [P3] 监控 / 链路追踪                    → 运维能力
```

---

## 六、关键技术风险

| 风险                                   | 影响 | 缓解措施                                         |
| -------------------------------------- | ---- | ------------------------------------------------ |
| IM 单机 ConnectionManager 无法水平扩展 | 高   | Sprint 2 优先实现 Redis 分布式路由               |
| Touch 模块无实际发送能力               | 高   | Sprint 1 第一周完成 Provider 接入                |
| 大量空 ServiceImpl 导致接口空转        | 中   | 梳理哪些 ApplicationService 已覆盖，补全缺失逻辑 |
| MQ 消费者缺失导致权限事件积压          | 中   | Sprint 1 中验证 RabbitMQ 消费端正常工作          |
| Admin 长期空置影响运营                 | 中   | Sprint 2 建立基础管理能力                        |
| 无 Token 黑名单存在安全隐患            | 高   | Sprint 1 优先修复                                |
