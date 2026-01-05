# 权限变更事件系统测试指南

## 概述

本文档说明如何测试权限变更事件系统，该系统使用 framework-starter-mq 实现 member-service 和 auth-service 之间的消息通信。

## 架构

- **member-service**: 权限数据的 owner，负责发布权限变更事件
- **auth-service**: 权限缓存的 consumer，监听权限变更事件并刷新缓存
- **RabbitMQ**: 消息中间件，传递权限变更事件

## 事件流程

### 1. 分配角色

```bash
POST http://localhost:11000/memberRole/assign?memberId=1&roleId=1&operator=admin

# 执行流程：
# 1. MemberRoleController创建member_member_role记录
# 2. PermissionChangeEventPublisher.publishMemberRoleAssigned()
# 3. 消息发送到RabbitMQ队列 "member.permission"
# 4. PermissionChangedEventListener接收事件
# 5. PermissionDomainService.invalidatePermissionCache()清除auth-service的权限缓存
```

### 2. 撤销角色

```bash
DELETE http://localhost:11000/memberRole/revoke?memberId=1&roleId=1&operator=admin
```

### 3. 为角色添加权限

```bash
POST http://localhost:11000/rolePermission/add?roleId=1&permissionId=100&operator=admin

# 注意：此操作会查找所有拥有该角色的会员，并为每个会员发布权限变更事件
```

### 4. 从角色移除权限

```bash
DELETE http://localhost:11000/rolePermission/remove?roleId=1&permissionId=100&operator=admin
```

## 环境配置

### 1. 启动 RabbitMQ

```bash
# 使用Docker启动
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# 访问管理界面: http://localhost:15672
# 用户名/密码: guest/guest
```

### 2. 配置环境变量 (.env)

```dotenv
# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

### 3. 启动服务

```bash
# 1. 启动member-service
cd member/member-service
mvn spring-boot:run

# 2. 启动auth-service
cd auth/auth-service
mvn spring-boot:run
```

## 验证步骤

### 1. 检查 RabbitMQ 连接

打开 RabbitMQ 管理界面，检查：

- Connections: 应该看到来自 member-service 和 auth-service 的连接
- Exchanges: 应该自动创建 "member.permission" exchange
- Queues: 应该自动创建 "member.permission" queue

### 2. 测试角色分配

```bash
# 分配角色
curl -X POST "http://localhost:11000/memberRole/assign?memberId=1&roleId=1&operator=admin"

# 检查日志：
# member-service: "Published MEMBER_ROLE_ASSIGNED event for member: 1, role: 1"
# auth-service: "[权限变更监听] 收到权限变更事件: changeType=MEMBER_ROLE_ASSIGNED"
# auth-service: "[权限变更监听] 成功清除1个会员的权限缓存"
```

### 3. 测试角色权限变更

```bash
# 先创建一些测试数据
# 会员1拥有角色1
# 会员2拥有角色1

# 为角色1添加权限100
curl -X POST "http://localhost:11000/rolePermission/add?roleId=1&permissionId=100&operator=admin"

# 检查日志：
# member-service: "为角色添加权限成功: roleId=1, permissionId=100, affectedMembers=2"
# auth-service: "[权限变更监听] 收到权限变更事件: changeType=ROLE_PERMISSION_ADDED, affectedMembers=2"
# auth-service: "[权限变更监听] 成功清除2个会员的权限缓存"
```

## 监控和调试

### 查看消息队列状态

```bash
# 使用rabbitmqctl
docker exec rabbitmq rabbitmqctl list_queues

# 查看队列详情
docker exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

### 日志关键点

- member-service: `PermissionChangeEventPublisher`
- framework-starter-mq: `RabbitMQMessageProducer`, `MessageConsumerRegistry`
- auth-service: `PermissionChangedEventListener`
- auth-service: `PermissionDomainService`

## 故障排查

### 1. 消息未发送

- 检查 RabbitMQ 是否运行
- 检查 member-service 的 RabbitMQ 配置
- 查看 member-service 日志是否有连接错误

### 2. 消息未消费

- 检查 auth-service 的 RabbitMQ 配置
- 检查 auth-service 日志是否有 MessageConsumerRegistry 注册日志
- 确认队列名称匹配（"member.permission"）

### 3. 消息消费失败

- 查看 auth-service 的 PermissionChangedEventListener 日志
- 检查 JSON 序列化/反序列化问题
- 确认 PermissionChangedEvent 字段在两个服务中定义一致

## 性能注意事项

- 角色权限变更可能影响大量用户，事件处理是异步的
- 缓存失效是批量操作，但逐个用户处理
- 考虑使用 Redis Pipeline 或批量删除优化性能
- 消息重试策略由 RabbitMQ 配置控制

## 扩展建议

1. 添加死信队列处理失败消息
2. 实现消息重试机制
3. 添加消息追踪和审计
4. 实现消息幂等性处理
5. 添加性能监控（消息延迟、处理时间等）
