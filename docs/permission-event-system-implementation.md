# Permission Change Event System — Implementation Summary

## Overview

A message-queue-based permission change event system has been implemented to address the problem of stale permission caches in `auth-service` after permission data changes in `member-service`.

## Technical Architecture

### Core Components

1. **framework-starter-mq**: Message queue abstraction layer
   - Supports RabbitMQ and Kafka
   - Provides a unified `MessageTemplate` API
   - Auto-configuration and consumer registration

2. **member-service**: Permission event publisher
   - `PermissionChangedEvent`: permission change domain event
   - `PermissionChangeEventPublisher`: event publisher
   - `MemberRoleController`: role assignment/revocation API
   - `RolePermissionController`: role-permission management API

3. **auth-service**: Permission cache manager
   - `PermissionChangedEventListener`: event listener
   - `PermissionDomainService`: cache invalidation logic

## Created Files

### framework-starter-mq Module

```
framework/framework-starter-mq/
├── src/main/java/com/pot/zing/framework/mq/
│   ├── core/
│   │   ├── DomainEvent.java              # Domain event interface
│   │   ├── AbstractDomainEvent.java       # Abstract domain event base class
│   │   ├── MessageProducer.java           # Message producer interface
│   │   ├── MessageConsumer.java           # Message consumer interface
│   │   └── MessageTemplate.java           # Message template (analogous to RedisTemplate)
│   ├── adapter/
│   │   ├── rabbitmq/
│   │   │   └── RabbitMQMessageProducer.java
│   │   └── kafka/
│   │       └── KafkaMessageProducer.java
│   └── config/
│       ├── MQAutoConfiguration.java       # Auto-configuration
│       └── MQProperties.java              # Configuration properties
└── src/main/resources/
    └── META-INF/
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### member-service Changes

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
│   └── application.yml                           # UPDATED (RabbitMQ config added)
└── pom.xml                                       # UPDATED (framework-starter-mq dependency added)
```

### auth-service Changes

```
auth/auth-service/
├── src/main/java/com/pot/auth/
│   └── infrastructure/
│       ├── event/
│       │   └── PermissionChangedEvent.java       # NEW
│       └── listener/
│           └── PermissionChangedEventListener.java # NEW
├── src/main/resources/
│   └── application.yml                           # UPDATED (RabbitMQ config added)
└── pom.xml                                       # UPDATED (framework-starter-mq dependency added)
```

### Documentation

```
docs/
└── permission-event-system-test.md               # NEW
```

## Event Types

### PermissionChangedEvent.ChangeType

- `MEMBER_ROLE_ASSIGNED`: member role was assigned
- `MEMBER_ROLE_REVOKED`: member role was revoked
- `ROLE_PERMISSION_ADDED`: permission added to a role
- `ROLE_PERMISSION_REMOVED`: permission removed from a role
- `ROLE_UPDATED`: role definition updated
- `PERMISSION_UPDATED`: permission definition updated

## API Endpoints

### member-service

```
POST   /memberRole/assign      # Assign a role to a member
DELETE /memberRole/revoke      # Revoke a role from a member
POST   /rolePermission/add     # Add a permission to a role
DELETE /rolePermission/remove  # Remove a permission from a role
```

## Configuration Requirements

### Environment Variables (.env)

```dotenv
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

### application.yml (auto-configured)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
```

## Workflow

### 1. Role Assignment Flow

```
User calls API
  ↓
MemberRoleController.assignRole()
  ↓
Persist member_member_role record
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
Evict Redis cache entry
```

### 2. Role Permission Change Flow

```
User calls API
  ↓
RolePermissionController.addPermission()
  ↓
Persist member_role_permission record
  ↓
Query all members holding the role
  ↓
PermissionChangeEventPublisher.publishRolePermissionAdded()
  ↓
Event payload includes affectedMemberIds (potentially multiple members)
  ↓
PermissionChangedEventListener.consume()
  ↓
Iterate affectedMemberIds and invalidate cache for each member
```

## Advantages

### 1. Real-Time Invalidation

- Permission changes immediately notify `auth-service` via the message queue.
- No waiting for cache TTL expiry; users see up-to-date permissions on their next request.

### 2. Decoupling

- `member-service` and `auth-service` are decoupled through the message queue.
- `member-service` has no knowledge of `auth-service`.
- `auth-service` does not poll or call `member-service` directly.

### 3. Scalability

- All `auth-service` instances receive the event.
- Additional consumers can be added to listen for permission change events with minimal effort.
- Switching to Kafka for higher throughput is straightforward.

### 4. Reliability

- RabbitMQ guarantees message durability.
- Consumer exceptions do not cause message loss (retry mechanism).
- Cache invalidation failures do not affect business logic (cache is reloaded on the next request).

## Considerations

### 1. Message Ordering

- Multiple permission changes for the same member may arrive out of order.
- Resolution: use timestamps or version numbers for optimistic concurrency control.
- Current approach: invalidate the cache directly; the next request reloads it (simplest and most effective).

### 2. Performance Considerations

- Large-scale role permission changes may affect many users.
- Event payloads include all `affectedMemberIds` (potentially large).
- Optimization options: batch cache eviction, Redis Pipeline.

### 3. Error Handling

- Cache invalidation failures do not throw exceptions (to avoid message re-delivery).
- Errors are logged for subsequent investigation.
- Cache TTL acts as the final safety net (eventual consistency).

### 4. Idempotency

- The current implementation is naturally idempotent (deleting a non-existent cache key is a no-op).
- If additional side effects are introduced in the future, idempotency must be explicitly considered.

## Next Steps

### 1. Performance Optimization

- [ ] Use Redis Pipeline for batch cache deletion
- [ ] Process large `affectedMemberIds` in chunks
- [ ] Add message processing performance monitoring

### 2. Reliability Enhancement

- [ ] Add a dead-letter queue for failed messages
- [ ] Implement a message retry strategy
- [ ] Add message tracing and audit logging

### 3. Feature Extension

- [ ] Support more permission change scenarios (Permission update, Role update, etc.)
- [ ] Implement permission change event history
- [ ] Add permission change notifications (email / in-app message)

### 4. Observability and Operations

- [ ] Add message processing latency monitoring
- [ ] Add queue backlog alerting
- [ ] Implement RabbitMQ health checks

## Testing Guide

See: [permission-event-system-test.md](permission-event-system-test.md)
