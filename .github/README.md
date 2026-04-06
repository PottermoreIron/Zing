# Zing AI Customization Guide

## 目标

这个目录存放 Zing 仓库的 Copilot 定制文件。目标不是把所有知识堆进一个文件，而是把常驻上下文、文件级规则和任务级工作流分层管理，降低冲突、重复和上下文污染。

## 结构约定

| 路径                             | 类型                  | 作用                                   | 使用时机                       |
| -------------------------------- | --------------------- | -------------------------------------- | ------------------------------ |
| `copilot-instructions.md`        | Workspace instruction | 唯一的仓库级常驻指令                   | 所有任务默认生效               |
| `instructions/*.instructions.md` | File instructions     | 面向文件类型或关注点的局部规则         | 修改匹配文件或命中描述触发词时 |
| `skills/*/SKILL.md`              | Skills                | 面向可复用任务流的步骤、模板和参考资料 | 遇到重复性工作流时             |
| `agents/*.agent.md`              | Custom agents         | 面向专门角色、工具边界和子代理委派     | 只有需要专门 agent 时          |

## 重要决策

- 当前仓库使用 `copilot-instructions.md` 作为唯一 workspace-wide instruction。
- 不同时引入 `AGENTS.md` 作为第二份 workspace instruction，避免优先级冲突和上下文重复。
- 如果未来需要按目录覆盖默认规则，例如 `auth/` 和 `im/` 形成明显不同的默认开发方式，应整体迁移到 `AGENTS.md` 体系，而不是与 `copilot-instructions.md` 并存。
- `.agent.md` 是“定制代理”，不是“索引页”或“目录页”。索引应维护在当前文件，agent 只用于专门角色与受限工作流。

## 项目概览

### 项目做什么

- Zing 是一个多模块 Java 后端平台，覆盖认证授权、会员域、即时通讯、管理后台和 API 网关。
- `framework` 提供基础能力封装，包括公共组件、分布式 ID、Redis、消息队列、限流、触达和代码生成。
- `auth`、`member` 是当前 DDD/六边形架构实践最核心的业务模块，`im`、`admin`、`gateway` 分别承载实时通信、运营后台和统一流量入口。

### 核心架构

- 形态：Maven 多模块 monorepo。
- 风格：Spring Boot 微服务 + DDD/Hexagonal + MyBatis-Plus + 事件驱动协作。
- 默认分层：`interfaces -> application -> domain <- infrastructure`。
- 跨模块协作：优先 facade 或事件，不做跨模块持久化耦合。

### 已验证技术栈

- Java 21
- Spring Boot 3.4.2
- Spring Cloud 2024.0.2
- Spring Cloud Alibaba Nacos Discovery 2023.0.3.3
- MyBatis-Plus 3.5.12
- MySQL + Redis + Flyway
- RabbitMQ/Kafka 抽象 starter
- Netty 4.2.3.Final
- JJWT 0.12.6
- SpringDoc OpenAPI 2.8.9
- Leaf 1.0.1-RELEASE
- weixin-java-mp 4.7.7.B

## 模块地图

| 模块            | 角色                                   |
| --------------- | -------------------------------------- |
| `dependencies/` | 统一 BOM 与版本管理                    |
| `framework/`    | 公共基础设施与通用 starter             |
| `auth/`         | 认证、授权、Token、权限缓存与鉴权协作  |
| `member/`       | 会员、RBAC、设备、社交账号等领域能力   |
| `im/`           | 即时通讯协议、连接管理、消息与会话领域 |
| `admin/`        | 管理后台能力                           |
| `gateway/`      | API 网关与统一流量入口                 |

## 当前 Instructions 目录

| 文件                                        | 关注点                                      | 典型触发词                                        |
| ------------------------------------------- | ------------------------------------------- | ------------------------------------------------- |
| `instructions/api-openapi.instructions.md`  | REST 控制器、OpenAPI 契约、统一响应         | `REST`、`controller`、`OpenAPI`、`接口`、`控制器` |
| `instructions/architecture.instructions.md` | DDD 分层、模块边界、assembler、port、facade | `DDD`、`aggregate`、`repository`、`聚合`、`仓储`  |
| `instructions/coding.instructions.md`       | 仓库级编码优先级、命名、变更粒度            | `coding standard`、`规范`、`命名`、`重构`         |
| `instructions/commenting.instructions.md`   | 注释策略、Javadoc、英文注释、失效注释治理   | `comment`、`Javadoc`、`注释`、`文档注释`          |
| `instructions/java.instructions.md`         | Java 语言约束、异常、Optional、日志         | `Java`、`Optional`、`record`、`异常`、`日志`      |
| `instructions/persistence.instructions.md`  | MyBatis、SQL、安全性、性能                  | `MyBatis`、`mapper XML`、`SQL`、`持久化`          |
| `instructions/spring-boot.instructions.md`  | Spring Boot 组件、校验、事务、配置          | `@Service`、`@Transactional`、`配置`、`事务`      |
| `instructions/testing.instructions.md`      | 测试分层、Mock、行为断言                    | `unit test`、`Mockito`、`单元测试`、`断言`        |

## 当前 Skills 目录

| Skill                    | 用途                                           | 典型触发词                                       |
| ------------------------ | ---------------------------------------------- | ------------------------------------------------ |
| `skills/ddd-java/`       | 新增聚合、值对象、仓储接口、应用服务、DDD 重构 | `DDD`、`aggregate`、`聚合`、`值对象`、`领域事件` |
| `skills/java-testing/`   | 领域模型、应用服务、基础设施测试编写           | `test`、`Mockito`、`AssertJ`、`单元测试`         |
| `skills/mybatis-mapper/` | Mapper XML、PO、Mapper 接口与 `resultMap` 修复 | `Mapper XML`、`resultMap`、`PO`、`xml映射`       |

## 何时新增 .agent.md

满足以下任一条件时，再考虑在 `.github/agents/` 下新增 custom agent：

- 需要稳定的专门角色，例如“DDD 设计评审”“只读架构巡检”“Mapper XML 修复代理”。
- 需要明确工具边界，例如只允许 `read/search`，或禁止 terminal/edit。
- 需要被主 agent 反复委派，且输出格式相对固定。

以下场景不要使用 `.agent.md`：

- 项目概览或目录索引
- 通用编码规范
- 简单模板收纳
- 只是为了给现有 instruction/skill 做导航

## 维护规则

- `copilot-instructions.md` 只放所有任务都依赖的事实与不变量，不复制 skill 模板和细节规范。
- `description` 统一使用 “Use when...” 风格，并尽量包含中英文触发词，方便双语工作流发现。
- `applyTo` 只匹配真正需要自动注入的文件，不要为了省事放大到过宽范围。
- instruction 保持单关注点、短而准；skill 保持可执行步骤、模板和参考资料。
- 技术版本、模块职责、架构边界发生变化时，同时更新本文件与 `copilot-instructions.md`。
