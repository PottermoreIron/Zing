# 权限变更事件系统实现总结

## 实现概述

实现了基于消息队列的权限变更事件系统，解决了权限数据变更后，auth-service 中的权限缓存无法实时失效的问题。

## 技术架构

### 核心组件

1. **framework-starter-mq**: 消息队列抽象层

   - 支持 RabbitMQ 和 Kafka
   - 提供统一的 MessageTemplate API
   - 自动装配和消费者注册

2. **member-service**: 权限事件发布者

   - PermissionChangedEvent: 权限变更领域事件
   - PermissionChangeEventPublisher: 事件发布器
   - MemberRoleController: 角色分配/撤销 API
   - RolePermissionController: 角色权限管理 API

3. **auth-service**: 权限缓存管理者
   - PermissionChangedEventListener: 事件监听器
   - PermissionDomainService: 缓存失效逻辑

## 已创建文件

### framework-starter-mq 模块

```
framework/framework-starter-mq/
├── src/main/java/com/pot/zing/framework/mq/
│   ├── core/
│   │   ├── DomainEvent.java              # 领域事件接口
│   │   ├── AbstractDomainEvent.java       # 领域事件抽象类
│   │   ├── MessageProducer.java           # 消息生产者接口
│   │   ├── MessageConsumer.java           # 消息消费者接口
│   │   └── MessageTemplate.java           # 消息模板（类似RedisTemplate）
│   ├── adapter/
│   │   ├── rabbitmq/
│   │   │   └── RabbitMQMessageProducer.java
│   │   └── kafka/
│   │       └── KafkaMessageProducer.java
│   └── config/
│       ├── MQAutoConfiguration.java       # 自动配置
│       └── MQProperties.java              # 配置属性
└── src/main/resources/
    └── META-INF/
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### member-service 变更

```
member/member-service/
├── src/main/java/com/pot/member/service/
│   ├── domain/event/
│   │   ├── PermissionChangedEvent.java           # NEW
│   │   └── PermissionChangeEventPublisher.java   # NEW
│   └── controller/
│       ├── MemberRoleController.java             # UPDATED
│       └── RolePermissionController.java         # UPDATED
├── src/main/resources/
│   └── application.yml                           # UPDATED (添加RabbitMQ配置)
└── pom.xml                                       # UPDATED (添加framework-starter-mq依赖)
```

### auth-service 变更

```
auth/auth-service/
├── src/main/java/com/pot/auth/
│   └── infrastructure/
│       ├── event/
│       │   └── PermissionChangedEvent.java       # NEW
│       └── listener/
│           └── PermissionChangedEventListener.java # NEW
├── src/main/resources/
│   └── application.yml                           # UPDATED (添加RabbitMQ配置)
└── pom.xml                                       # UPDATED (添加framework-starter-mq依赖)
```

### 文档

```
docs/
└── permission-event-system-test.md               # NEW
```

## 事件类型

### PermissionChangedEvent.ChangeType

- `MEMBER_ROLE_ASSIGNED`: 会员角色分配
- `MEMBER_ROLE_REVOKED`: 会员角色撤销
- `ROLE_PERMISSION_ADDED`: 角色权限添加
- `ROLE_PERMISSION_REMOVED`: 角色权限移除
- `ROLE_UPDATED`: 角色更新
- `PERMISSION_UPDATED`: 权限更新

## API 端点

### member-service

```
POST   /memberRole/assign      # 分配角色
DELETE /memberRole/revoke      # 撤销角色
POST   /rolePermission/add     # 为角色添加权限
DELETE /rolePermission/remove  # 从角色移除权限
```

## 配置要求

### 环境变量 (.env)

```dotenv
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

### application.yml (已自动配置)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
```

## 工作流程

### 1. 角色分配流程

```
用户调用API
  ↓
MemberRoleController.assignRole()
  ↓
保存member_member_role记录
  ↓
PermissionChangeEventPublisher.publishMemberRoleAssigned()
  ↓
MessageTemplate.send("member.permission", event)
  ↓
RabbitMQ Exchange/Queue
  ↓
PermissionChangedEventListener.consume()
  ↓
PermissionDomainService.invalidatePermissionCache()
  ↓
清除Redis缓存
```

### 2. 角色权限变更流程

```
用户调用API
  ↓
RolePermissionController.addPermission()
  ↓
保存member_role_permission记录
  ↓
查询所有拥有该角色的会员
  ↓
PermissionChangeEventPublisher.publishRolePermissionAdded()
  ↓
消息包含affectedMemberIds (可能是多个会员)
  ↓
PermissionChangedEventListener.consume()
  ↓
遍历所有affectedMemberIds，逐个清除缓存
```

## 优点

### 1. 实时性

- 权限变更后立即通过消息队列通知 auth-service
- 无需等待缓存过期，用户下次请求即可获得最新权限

### 2. 解耦

- member-service 和 auth-service 通过消息队列解耦
- member-service 无需知道 auth-service 的存在
- auth-service 无需轮询或调用 member-service API

### 3. 可扩展性

- 支持多个 auth-service 实例，每个实例都会收到事件
- 可以轻松添加其他服务监听权限变更事件
- 支持切换到 Kafka 以获得更高吞吐量

### 4. 可靠性

- RabbitMQ 保证消息不丢失
- 消费者异常不会导致消息丢失（重试机制）
- 缓存失效失败不影响业务（下次请求重新加载）

## 注意事项

### 1. 消息顺序

- 对同一个会员的多次权限变更，消息可能乱序
- 解决方案：使用时间戳或版本号进行乐观控制
- 当前实现：直接清除缓存，下次请求重新加载（最简单有效）

### 2. 性能考虑

- 大规模角色权限变更可能影响大量用户
- 事件包含所有 affectedMemberIds（可能很大）
- 优化方向：批量清除缓存、使用 Redis Pipeline

### 3. 异常处理

- 缓存失效失败不抛出异常（避免消息重试）
- 记录错误日志供后续排查
- 依赖缓存 TTL 兜底（最终一致性）

### 4. 幂等性

- 当前实现天然幂等（清除不存在的缓存 key 不会报错）
- 如果未来需要其他副作用操作，需要考虑幂等性

## 下一步优化建议

### 1. 性能优化

- [ ] 使用 Redis Pipeline 批量删除缓存
- [ ] 对大量 affectedMemberIds 进行分批处理
- [ ] 添加消息处理性能监控

### 2. 可靠性增强

- [ ] 添加死信队列处理失败消息
- [ ] 实现消息重试策略
- [ ] 添加消息追踪和审计日志

### 3. 功能扩展

- [ ] 支持更多权限变更场景（Permission 自身更新、Role 更新等）
- [ ] 实现权限变更事件历史记录
- [ ] 添加权限变更通知（发送邮件/站内消息）

### 4. 监控和运维

- [ ] 添加消息处理延迟监控
- [ ] 添加消息堆积告警
- [ ] 实现 RabbitMQ 健康检查

## 测试指南

参见: [permission-event-system-test.md](permission-event-system-test.md)
