# Zing 项目 Copilot 全局指令

## 文件定位

- 当前仓库只使用 `.github/copilot-instructions.md` 作为 workspace-wide instruction。
- `.github/README.md` 是 `.github` 目录的索引与项目概览；更细的文件级规则放在 `.github/instructions/`，可复用工作流放在 `.github/skills/`。
- 本文件只保留所有任务都需要的稳定事实、核心边界与仓库不变量，不再堆叠模板和细节规则。

## 项目概览

- Zing 是一个 Java 21 多模块后端平台，覆盖认证授权、会员、即时通讯、管理后台和 API 网关。
- 仓库采用 Maven monorepo 结构，核心模块包括 `dependencies`、`framework`、`auth`、`member`、`im`、`admin`、`gateway`。
- 主要技术栈基线：Spring Boot 3.4.2、Spring Cloud 2024.0.2、MyBatis-Plus 3.5.12、MySQL、Redis、RabbitMQ/Kafka 抽象、Netty、SpringDoc、Leaf ID、JJWT。

## 默认架构

- 领域复杂模块优先采用 DDD/Hexagonal：`interfaces -> application -> domain <- infrastructure`。
- `domain` 只放聚合、实体、值对象、领域服务、领域事件、端口，不引入 Spring 或 MyBatis 依赖。
- `application` 负责用例编排、事务边界、命令、查询、DTO 与 assembler，不承载核心业务规则。
- `infrastructure` 负责仓储实现、Mapper、外部适配器、配置、消息与 ID 适配。
- `interfaces` 只处理 transport concern，如 REST 控制器、内部接口、消费者和调度入口。
- 跨模块协作必须走 facade 或事件，不做跨模块持久化耦合。

## 仓库不变量

- 用户显示名统一使用 `nickname`，不使用 `username`。
- 业务 ID 由 ID 生成器产生，不使用数据库自增。
- 数据库表名使用单数。
- 持久化实体位于 `infrastructure.persistence.entity`。
- Controller 只处理协议层，事务和编排放在应用服务。
- 统一使用构造器注入，禁止字段 `@Autowired`。
- REST 返回统一 `Result<T>`。

## 使用规则

- 优先做最小且完整的改动，修复根因，不打表面补丁。
- 修改具体文件前，必须读取匹配的 `.github/instructions/*.instructions.md`。
- 遇到聚合建模、Java 测试、MyBatis Mapper 维护等可复用流程时，优先加载对应 skill。
- 模块职责、技术版本、架构边界发生变化时，同时更新本文件与 `.github/README.md`。
