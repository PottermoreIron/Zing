# Zing 项目 Copilot 全局指令

## 项目概览

- **名称**: Zing —— 多模块 Spring Boot 微服务平台
- **语言**: Java 17+
- **框架**: Spring Boot 3、MyBatis-Plus、Lombok、RabbitMQ
- **测试**: JUnit 5、Mockito、AssertJ
- **架构**: DDD（Domain-Driven Design）

## 模块结构

```
zing/
├── framework/          # 公共基础设施（common、id、mq、ratelimit、redis）
├── auth/               # 认证授权服务
├── member/             # 会员服务（member-facade + member-service）
├── im/                 # 即时通讯
├── admin/              # 管理后台
└── gateway/            # API 网关
```

## DDD 分层约定

每个服务的包根路径为 `com.pot.{module}.service`，分层结构：

```
domain/
  model/{aggregate}/    # 聚合根、值对象、实体
  repository/           # 仓储接口（领域层定义）
  service/              # 领域服务
  event/                # 领域事件
  port/                 # 出站端口（如 PasswordEncoder、IdGenerator）
application/
  service/              # 应用服务（编排领域对象）
  command/              # 命令对象（写操作入参）
  query/                # 查询对象（读操作入参）
  dto/                  # 输出 DTO
  assembler/            # 领域对象 ↔ DTO 转换
infrastructure/
  persistence/
    entity/             # MyBatis-Plus PO（数据库映射对象）
    mapper/             # MyBatis Mapper 接口
    repository/         # 仓储实现
  config/               # Spring 配置
  event/                # 事件发布适配器
  id/                   # ID 生成器适配器
interfaces/
  rest/                 # REST 控制器
  rest/internal/        # 内部 Feign 接口实现
```

## 命名约定

| 场景       | 规则                       | 示例                                   |
| ---------- | -------------------------- | -------------------------------------- |
| 用户显示名 | `nickname`（非 username）  | `Nickname.of("john")`                  |
| 真实姓名   | `firstName` + `lastName`   | —                                      |
| 业务 ID    | 自定义生成（非数据库自增） | `MemberId`、`RoleId`                   |
| 数据库表名 | **单数**                   | `member`、`role`、`permission`         |
| PO 类名    | 与表名对应，无后缀         | `Member.java`（在 persistence/entity） |
| 聚合根     | `{Name}Aggregate`          | `MemberAggregate`                      |
| 值对象     | 直接描述概念               | `Email`、`Nickname`、`MemberId`        |

## 代码规范

### 领域层

- 聚合根**不继承**任何基类，使用**工厂方法**（`create()`、`reconstitute()`）
- 领域层**禁止** Spring 注解（`@Service`、`@Component` 等）
- 值对象不可变，使用静态工厂方法 `of(...)` 创建
- 状态变更产生领域事件，存入聚合根内部列表，由应用层提取并发布

### 应用层

- 应用服务用 `@Service`，通过构造注入依赖
- 一个应用服务只编排领域对象，不包含业务逻辑
- 事务边界在应用服务，用 `@Transactional`

### 基础设施层

- PO 使用 MyBatis-Plus 注解（`@TableName`、`@TableId(type = IdType.INPUT)`）
- Mapper XML 放在 `src/main/resources/mapper/`，`resultMap type` 必须引用 `infrastructure.persistence.entity` 包
- 仓储实现负责 PO ↔ 领域对象转换

### 接口层

- Controller 只处理 HTTP 相关逻辑（参数解析、响应封装），不含业务逻辑
- 内部 Feign 实现放在 `rest/internal/`，实现 facade 模块定义的接口

## 禁止事项

- ❌ 不在 `domain/` 包中引入 Spring / MyBatis / Lombok（Lombok 注解除外）
- ❌ 不在 domain 层使用数据库实体（PO），必须通过仓储接口隔离
- ❌ 不在 Service 字段中使用 `@Autowired`，统一用构造器注入
- ❌ 不使用 `username` 字段（已弃用，用 `nickname`）
- ❌ 不使用数据库自增 ID，所有业务 ID 由 ID 生成器创建
