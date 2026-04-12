# Permission Change Event System — Test Guide

## Overview

This document describes how to test the permission change event system, which uses `framework-starter-mq` to enable message-based communication between `member-service` and `auth-service`.

## Architecture

- **member-service**: Owner of permission data; responsible for publishing permission change events.
- **auth-service**: Consumer of permission cache; listens for permission change events and refreshes the cache.
- **RabbitMQ**: Message broker that delivers permission change events.

## Event Flow

### 1. Assign Role

```bash
POST http://localhost:11000/memberRole/assign?memberId=1&roleId=1&operator=admin

# Execution flow:
# 1. MemberRoleController creates a member_member_role record
# 2. PermissionChangeEventPublisher.publishMemberRoleAssigned()
# 3. Message sent to RabbitMQ queue "member.permission"
# 4. PermissionChangedEventListener receives the event
# 5. PermissionDomainService.invalidatePermissionCache() evicts the auth-service permission cache
```

### 2. Revoke Role

```bash
DELETE http://localhost:11000/memberRole/revoke?memberId=1&roleId=1&operator=admin
```

### 3. Add Permission to Role

```bash
POST http://localhost:11000/rolePermission/add?roleId=1&permissionId=100&operator=admin

# Note: This operation queries all members holding the role and publishes a permission change event for each.
```

### 4. Remove Permission from Role

```bash
DELETE http://localhost:11000/rolePermission/remove?roleId=1&permissionId=100&operator=admin
```

## Environment Setup

### 1. Start RabbitMQ

```bash
# Start with Docker
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# Management UI: http://localhost:15672
# Credentials: guest / guest
```

### 2. Configure Environment Variables (.env)

```dotenv
# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

### 3. Start Services

```bash
# 1. Start member-service
cd member/member-service
mvn spring-boot:run

# 2. Start auth-service
cd auth/auth-service
mvn spring-boot:run
```

## Verification Steps

### 1. Check RabbitMQ Connections

Open the RabbitMQ management UI and verify:

- **Connections**: connections from both `member-service` and `auth-service` should be visible.
- **Exchanges**: the `member.permission` exchange should be created automatically.
- **Queues**: the `member.permission` queue should be created automatically.

### 2. Test Role Assignment

```bash
# Assign a role
curl -X POST "http://localhost:11000/memberRole/assign?memberId=1&roleId=1&operator=admin"

# Expected log entries:
# member-service: "Published MEMBER_ROLE_ASSIGNED event for member: 1, role: 1"
# auth-service: "[Permission] Permission change event received: changeType=MEMBER_ROLE_ASSIGNED"
# auth-service: "[Permission] Successfully invalidated cache for 1 member(s)"
```

### 3. Test Role Permission Change

```bash
# Prerequisite test data:
# - Member 1 has role 1
# - Member 2 has role 1

# Add permission 100 to role 1
curl -X POST "http://localhost:11000/rolePermission/add?roleId=1&permissionId=100&operator=admin"

# Expected log entries:
# member-service: "Permission added to role: roleId=1, permissionId=100, affectedMembers=2"
# auth-service: "[Permission] Permission change event received: changeType=ROLE_PERMISSION_ADDED, affectedMembers=2"
# auth-service: "[Permission] Successfully invalidated cache for 2 member(s)"
```

## Monitoring and Debugging

### Check Queue Status

```bash
# Using rabbitmqctl
docker exec rabbitmq rabbitmqctl list_queues

# Queue details
docker exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

### Key Log Points

- **member-service**: `PermissionChangeEventPublisher`
- **framework-starter-mq**: `RabbitMQMessageProducer`, `MessageConsumerRegistry`
- **auth-service**: `PermissionChangedEventListener`
- **auth-service**: `PermissionDomainService`

## Troubleshooting

### 1. Message Not Sent

- Verify RabbitMQ is running.
- Check the RabbitMQ configuration in `member-service`.
- Look for connection errors in `member-service` logs.

### 2. Message Not Consumed

- Check the RabbitMQ configuration in `auth-service`.
- Look for `MessageConsumerRegistry` registration logs in `auth-service`.
- Confirm the queue name matches (`"member.permission"`).

### 3. Message Consumption Failure

- Review `PermissionChangedEventListener` logs in `auth-service`.
- Check for JSON serialization/deserialization issues.
- Ensure the `PermissionChangedEvent` fields are defined consistently in both services.

## Performance Considerations

- Role permission changes can affect a large number of users; event processing is asynchronous.
- Cache invalidation is performed per-member, which may be slow for large member sets.
- Consider using Redis Pipeline or batch deletion to improve throughput.
- Message retry strategy is controlled by the RabbitMQ configuration.

## Extension Recommendations

1. Add a dead-letter queue to handle failed messages.
2. Implement a message retry mechanism.
3. Add message tracing and auditing.
4. Implement idempotent message processing.
5. Add performance monitoring (message latency, processing time, etc.).
