# Zing Project Architecture Analysis and Development Plan

> Generated: 2026-03-09

---

## 1. System Architecture Overview

### 1.1 Technology Stack

| Category          | Technology                                | Version       |
| ----------------- | ----------------------------------------- | ------------- |
| Language          | Java                                      | 21            |
| Framework         | Spring Boot                               | 3.4.2         |
| Microservices     | Spring Cloud                              | 2024.0.2      |
| Service Registry  | Spring Cloud Alibaba Nacos                | 2023.0.3.3    |
| ORM               | MyBatis-Plus                              | 3.5.12        |
| Database          | MySQL                                     | 9.2.0         |
| Cache             | Redis                                     | —             |
| Networking        | Netty                                     | 4.2.3.Final   |
| JWT               | JJWT                                      | 0.12.6        |
| Distributed ID    | Meituan Leaf                              | 1.0.1         |
| Message Queue     | RabbitMQ / Kafka (dual support)           | —             |
| API Documentation | SpringDoc OpenAPI                         | 2.8.9         |
| Third-party Login | weixin-java-mp (WeChat)                   | 4.7.7.B       |

### 1.2 Module Topology

```
zing (parent pom)
├── dependencies          # Version management BOM (unified dependency versions)
├── framework             # Framework foundation layer (custom starters)
│   ├── framework-common              # Common utilities (Result model, exceptions, utils)
│   ├── framework-starter-id          # Distributed ID (Meituan Leaf wrapper)
│   ├── framework-starter-redis       # Redis service wrapper
│   ├── framework-starter-ratelimit   # Rate limiting (Guava/Redis dual implementation)
│   ├── framework-starter-mq          # Message queue abstraction (Kafka/RabbitMQ dual adapter)
│   ├── framework-starter-touch       # Notification delivery (SMS/Email multi-channel)
│   └── framework-starter-code-generator  # Code generator
├── gateway               # API Gateway (Spring Cloud Gateway)
├── auth                  # Authentication and authorization service (DDD + Hexagonal)
│   ├── auth-facade       # API contract definitions
│   └── auth-service      # Core implementation
├── member                # Member service (DDD)
│   ├── member-facade     # API contract definitions
│   └── member-service    # Core implementation
├── im                    # Instant messaging service (Netty TCP + REST)
│   ├── im-facade         # API contract definitions
│   └── im-service        # Core implementation
└── admin                 # Administration back-office service (under development)
    ├── admin-facade
    └── admin-service
```

### 1.3 Request Flow

```
Client
  │
  ▼
Gateway (JWT validation + permission version check + header injection)
  │
  ├──▶ auth-service (login / registration / token refresh / verification code)
  │       └── calls member-service via MemberServiceClient (Feign)
  │
  ├──▶ member-service (member profile / RBAC / devices / social accounts)
  │       └── publishes permission change events to auth-service via RabbitMQ/Kafka
  │
  ├──▶ im-service (Netty TCP long connection + REST HTTP endpoints)
  │
  └──▶ admin-service (administration back-office, under development)
```

### 1.4 Core Design Patterns

| Module                       | Design Patterns                                                                                          |
| ---------------------------- | -------------------------------------------------------------------------------------------------------- |
| auth-service                 | DDD + Hexagonal (Ports & Adapters) + Strategy (login/registration/auth strategies) + Validation Chain   |
| member-service               | DDD (aggregate roots: MemberAggregate/RoleAggregate/PermissionAggregate) + Repository pattern           |
| im-service                   | Netty Pipeline + MessageProcessor factory pattern + ConnectionManager                                    |
| framework-starter-ratelimit  | AOP + Strategy pattern (IP/user/fixed key + Guava/Redis implementations)                                 |
| framework-starter-touch      | Template Method + Strategy pattern (channel selection + fallback)                                        |
| framework-starter-mq         | Adapter pattern (unified MessageTemplate API abstracting Kafka/RabbitMQ differences)                     |

---

## 2. Module Completion Score

### 2.1 Framework Layer ⭐⭐⭐⭐☆ (8/10)

| Sub-module                        | Status         | Notes                                                                   |
| --------------------------------- | -------------- | ----------------------------------------------------------------------- |
| framework-common                  | ✅ Complete    | Result model, global exception handler, and common utilities complete   |
| framework-starter-id              | ✅ Complete    | Leaf distributed ID wrapper complete with error handling                |
| framework-starter-redis           | ✅ Complete    | Redis service wrapper complete                                          |
| framework-starter-ratelimit       | ✅ Complete    | AOP annotation-based limiting; supports IP/user/fixed key; Guava+Redis  |
| framework-starter-mq              | ⚠️ Mostly done | Producer complete (Kafka/RabbitMQ); consumer registration present but no concrete consumer |
| framework-starter-touch           | ⚠️ Mostly done | Abstract layer complete with multi-channel fallback; **no actual SMS/Email provider** |
| framework-starter-code-generator  | ✅ Complete    | Code generation logic complete                                          |

**Points deducted:**

- No actual SMS provider (Aliyun / Tencent Cloud) or email provider (SMTP / SendGrid) implementation in the `touch` module.
- MQ consumer registration design is complete but no concrete business consumers.

---

### 2.2 Gateway ⭐⭐⭐☆☆ (6/10)

| Feature                    | Status       | Notes                                                        |
| -------------------------- | ------------ | ------------------------------------------------------------ |
| JWT token validation       | ✅ Complete  | RSA public key validation with full error handling           |
| Permission version check   | ✅ Complete  | Reads current version from Redis, compares with token        |
| Path allowlist             | ✅ Complete  | `/auth/login`, `/auth/register`, `/auth/refresh`             |
| User info header injection | ✅ Complete  | `X-User-Id`, `X-Perm-Version`, `X-Perm-Digest`              |
| Route configuration        | ❌ Not found | No `application.yml` routing rules (needs to be added)       |
| Service discovery          | ❌ Not found | Nacos service discovery integration unverified               |
| Circuit breaking           | ❌ Missing   | No Resilience4j / Sentinel configuration                     |
| Request trace propagation  | ❌ Missing   | No TraceId injection                                         |

---

### 2.3 Auth Service ⭐⭐⭐⭐☆ (7.5/10)

| Feature                          | Status        | Notes                                                                         |
| -------------------------------- | ------------- | ----------------------------------------------------------------------------- |
| Traditional login (4 methods)    | ✅ Complete   | Username+password, email+password, email OTP, phone OTP                       |
| Traditional registration (4)     | ✅ Complete   | Corresponding 4 registration methods                                          |
| One-stop authentication          | ✅ Complete   | Auto-detects register/login; supports 7 methods including OAuth2 and WeChat   |
| JWT token generation             | ✅ Complete   | Includes `perm_version` and `perm_digest` claims                              |
| Token refresh                    | ✅ Complete   | Refresh token mechanism                                                       |
| Verification code delivery       | ✅ Complete   | Email + SMS dual channel with rate limiting                                   |
| Endpoint rate limiting           | ✅ Complete   | All auth endpoints protected with IP-based rate limiting                      |
| Permission cache & version mgmt  | ✅ Complete   | Redis-cached permission set, version increment, digest computation            |
| Permission change event listener | ✅ Complete   | Consumes MQ events from member-service, invalidates cache                     |
| Permission check annotations     | ✅ Complete   | `@RequirePermission`, `@RequireRole`, `@RequireAnyPermission`                 |
| OAuth2 login implementation      | ⚠️ Interface  | Port fully defined; **no actual HTTP call implementation**                    |
| WeChat login implementation      | ⚠️ Interface  | Port fully defined; **no actual WeChat API integration**                      |
| Logout / token blocklist         | ❌ Missing    | No logout endpoint; tokens cannot be actively invalidated                     |
| Multi-device session management  | ❌ Missing    | No session count limits or forced logout logic                                |
| Admin domain authentication      | ⚠️ Stub       | `AdminModuleAdapter` present but not implemented                              |

---

### 2.4 Member Service ⭐⭐⭐☆☆ (6/10)

| Feature                    | Status       | Notes                                                                           |
| -------------------------- | ------------ | ------------------------------------------------------------------------------- |
| Member DDD aggregate root  | ✅ Complete  | `MemberAggregate` with complete domain methods                                  |
| Member basic CRUD          | ✅ Complete  | Via `MemberApplicationService` + Repository                                     |
| RBAC role-permission mgmt  | ✅ Complete  | Role/Permission/MemberRole CRUD + assignment/revocation                         |
| Permission change events   | ✅ Complete  | Publishes events via MQ after role/permission changes                           |
| Social account binding     | ✅ Complete  | `SocialConnection` + facade interface                                           |
| Device management          | ✅ Complete  | Device CRUD                                                                     |
| Internal auth endpoints    | ✅ Complete  | `InternalAuthController`, `InternalPermissionController`                        |
| Base service implementations | ⚠️ Skeleton | Most `ServiceImpl` classes extend only `ServiceImpl` with **no business logic** |
| Member list / search / page | ❌ Missing   | No member list query endpoint                                                   |
| Avatar upload              | ❌ Missing   | No OSS integration                                                              |
| Email/phone verification   | ❌ Missing   | No post-registration verification flow                                          |
| Member points / levels     | ❌ Not planned | Extension feature                                                             |

---

### 2.5 IM Service ⭐⭐☆☆☆ (4/10)

| Feature                     | Status        | Notes                                                        |
| --------------------------- | ------------- | ------------------------------------------------------------ |
| Netty TCP server            | ✅ Complete   | IMServer startup with graceful shutdown                      |
| Connection management       | ✅ Complete   | ConnectionManager (bidirectional user-Channel mapping)       |
| Custom binary protocol      | ✅ Complete   | ProtocolEncoder/Decoder, ProtocolHeader                      |
| Message processor factory   | ✅ Complete   | Multiple processors, priority ordering, async execution      |
| Auth processor              | ✅ Complete   | `AuthRequestProcessor`                                       |
| Heartbeat processor         | ✅ Complete   | `HeartbeatProcessor`                                         |
| DB entities & mappers       | ✅ Complete   | Message, Conversation, Friend, Group, etc. (10 tables)       |
| **Business service impls**  | ❌ **Empty**  | All `ServiceImpl` classes are empty, extending only `ServiceImpl` |
| **REST controller impls**   | ❌ **Empty**  | All controllers are empty                                    |
| Message routing / delivery  | ❌ Missing    | No actual message send logic                                 |
| Offline message storage     | ❌ Missing    | No offline message queue                                     |
| Read receipts               | ❌ Missing    | `MessageReadStatus` table exists but no logic                |
| Push notification           | ❌ Missing    | No APNs/FCM integration                                      |
| Distributed deployment      | ❌ Missing    | ConnectionManager is in-memory only; no Redis cluster routing |
| Group chat logic            | ❌ Missing    | Entities exist but no implementation                         |
| File transfer               | ❌ Missing    | File entity exists but no implementation                     |

---

### 2.6 Admin Service ⭐☆☆☆☆ (1/10)

| Feature            | Status          | Notes                                          |
| ------------------ | --------------- | ---------------------------------------------- |
| Project skeleton   | ✅ Present      | pom.xml, startup class, facade definitions     |
| Admin features     | ❌ **All missing** | No business logic implemented               |

---

## 3. Overall Completion Summary

| Module        | Score   | Weight | Weighted Score |
| ------------- | ------- | ------ | -------------- |
| Framework     | 8/10    | 15%    | 1.20           |
| Gateway       | 6/10    | 10%    | 0.60           |
| Auth          | 7.5/10  | 25%    | 1.875          |
| Member        | 6/10    | 20%    | 1.20           |
| IM            | 4/10    | 20%    | 0.80           |
| Admin         | 1/10    | 10%    | 0.10           |
| **Overall**   |         |        | **5.775 / 10** |

> **Summary:** The project architecture is clear and well-structured. The framework layer and authentication layer are of high quality. However, the IM service and Admin service are still in skeleton form, with a large amount of business logic remaining to be implemented.

---

## 4. Prioritized TODO List

### 🔴 P0 — Critical gaps; address immediately

| #   | Task                                          | Module         | Notes                                                                                                                       |
| --- | --------------------------------------------- | -------------- | --------------------------------------------------------------------------------------------------------------------------- |
| 1   | **Implement all IM service business logic**   | im-service     | FriendServiceImpl, MessageServiceImpl, GroupServiceImpl, ConversationServiceImpl, etc. (10 empty shells) — full implementation required |
| 2   | **Implement all IM REST controllers**         | im-service     | MessageController, FriendController, GroupController, etc. (10 empty controllers) — full CRUD required                      |
| 3   | **Implement IM message delivery routing**     | im-service     | Core feature: look up the target user's Channel on receive, push message; queue offline if target is disconnected           |
| 4   | **Implement Auth logout / token blocklist**   | auth-service   | Add `POST /auth/api/v1/logout`; record token JTI in Redis blocklist; check blocklist in Gateway filter                     |
| 5   | **Fix Member service empty implementations**  | member-service | Add business logic to base ServiceImpl classes, or clarify which operations are already handled by ApplicationService       |

---

### 🟠 P1 — Important features; complete soon

| #   | Task                                   | Module                   | Notes                                                                              |
| --- | -------------------------------------- | ------------------------ | ---------------------------------------------------------------------------------- |
| 6   | **Implement Touch SMS provider**       | framework-starter-touch  | Integrate Aliyun SMS / Tencent Cloud SMS; implement `AbstractSmsChannelImpl`       |
| 7   | **Implement Touch email provider**     | framework-starter-touch  | Integrate SMTP / SendGrid; implement `AbstractEmailChannelImpl`                    |
| 8   | **Implement OAuth2 login adapter**     | auth-service             | Implement `OAuth2Port`; handle authorization code exchange and user info retrieval for Google/GitHub |
| 9   | **Implement WeChat login adapter**     | auth-service             | Complete `WeChatPort`; obtain openid/unionid via weixin-java-mp                    |
| 10  | **Add Gateway route configuration**    | gateway                  | Write `application.yml` service routing rules; integrate Nacos service discovery  |
| 11  | **IM offline message support**         | im-service               | Queue messages in Redis or DB when target is offline; deliver on reconnection      |
| 12  | **Admin service baseline features**    | admin-service            | Implement member management (list/ban/unban), role-permission assignment, system configuration CRUD |

---

### 🟡 P2 — Important improvements; plan and complete

| #   | Task                                  | Module                      | Notes                                                                       |
| --- | ------------------------------------- | --------------------------- | --------------------------------------------------------------------------- |
| 13  | **IM distributed connection routing** | im-service                  | Store userId → service node mapping in Redis; implement cross-node routing  |
| 14  | **Multi-device session management**   | auth-service                | Embed device ID in token; support forced logout and session count limits    |
| 15  | **Member list query / search**        | member-service              | Paginated member list with filtering by status and registration date        |
| 16  | **Avatar / file upload (OSS)**        | member-service / im-service | Integrate object storage (Aliyun OSS / MinIO) for avatars and IM files     |
| 17  | **Gateway circuit breaking**          | gateway                     | Integrate Resilience4j; configure circuit breakers and rate limits for downstream services |
| 18  | **Gateway request tracing**           | gateway                     | Inject `X-Trace-Id`; support end-to-end distributed tracing with logs      |
| 19  | **Email/phone verification flow**     | member-service              | Send verification email/SMS after registration; update `gmt_email_verified_at` on confirmation |
| 20  | **MQ consumer implementation**        | framework-starter-mq        | Validate `MessageConsumerRegistry` works end-to-end for permission events  |

---

### 🟢 P3 — Quality improvements; continuous iteration

| #   | Task                         | Module               | Notes                                                               |
| --- | ---------------------------- | -------------------- | ------------------------------------------------------------------- |
| 21  | **Unit test coverage**       | Global               | Current test classes are empty; at minimum cover auth and member core logic |
| 22  | **Integration tests**        | auth / member        | Use Testcontainers for MySQL/Redis integration tests                |
| 23  | **OpenAPI documentation**    | auth / member / im   | Complete Swagger annotations on all controller methods              |
| 24  | **Docker Compose environment** | Global             | One-command dev environment (MySQL, Redis, Nacos, RabbitMQ)        |
| 25  | **Flyway database migration** | member / im          | Manage DB schema versions; eliminate manual SQL execution           |
| 26  | **Actuator / monitoring**    | Global               | Configure health checks and Prometheus metrics endpoints            |
| 27  | **IM read receipts**         | im-service           | Implement full `MessageReadStatus` logic with read/unread sync      |
| 28  | **Push notification**        | im-service           | Deliver offline message alerts via APNs/FCM                         |
| 29  | **Admin operational features** | admin-service      | Data dashboard, user behavior analytics, system announcements       |

---

## 5. Recommended Development Sequence (Sprint Plan)

### Sprint 1 — Core Feature Closure

```
Week 1–2:
  [P0] Touch SMS/Email provider implementation     → Unblock auth verification code delivery
  [P0] Auth logout / token blocklist               → Meet security baseline
  [P1] OAuth2 + WeChat login adapters              → Social login closure

Week 3–4:
  [P0] IM service business logic (1-on-1 chat first) → Core IM functionality usable
  [P0] IM REST controller implementation           → REST API available
  [P0] IM message routing + basic offline support  → Ready for integration testing
```

### Sprint 2 — Feature Completion

```
Week 5–6:
  [P1] Gateway routing + Nacos integration         → End-to-end microservice connectivity
  [P1] Admin service baseline features             → Management capability established
  [P2] Member list / search                        → Operational foundation

Week 7–8:
  [P2] OSS file upload                             → Avatar + IM file transfer
  [P2] IM distributed connection routing           → Horizontal scaling capability
  [P2] Multi-device session management             → Security and UX improvement
```

### Sprint 3 — Quality Assurance

```
Week 9–10:
  [P3] Unit tests + integration tests              → Quality baseline
  [P3] Docker Compose dev environment              → Developer efficiency
  [P3] OpenAPI documentation                       → Collaboration efficiency
  [P3] Monitoring / distributed tracing            → Operational readiness
```

---

## 6. Key Technical Risks

| Risk                                           | Impact | Mitigation                                                        |
| ---------------------------------------------- | ------ | ----------------------------------------------------------------- |
| IM single-node ConnectionManager cannot scale horizontally | High | Implement Redis-backed distributed routing in Sprint 2 |
| Touch module lacks actual delivery capability  | High   | Complete provider integration in Sprint 1, Week 1                |
| Numerous empty ServiceImpl classes cause silent failures | Medium | Audit which operations ApplicationService already covers; fill gaps |
| Missing MQ consumers cause permission event backlog | Medium | Verify RabbitMQ consumer operates correctly during Sprint 1     |
| Admin service absence impacts operations       | Medium | Establish baseline admin capabilities in Sprint 2                |
| No token blocklist poses security risk         | High   | Prioritize fix in Sprint 1                                        |
