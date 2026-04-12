# API Integration Test Report

**Date:** 2026-04-12  
**Environment:** macOS, localhost  
**Auth Service:** `http://localhost:8081`  
**Member Service:** `http://localhost:11000`  
**Test User:** `test_1775980539` / `Test@12345678` (memberId=4006)

---

## Infrastructure

| Component      | Status | Notes                                                       |
| -------------- | ------ | ----------------------------------------------------------- |
| Nacos          | ✅ UP  | port 8848, standalone mode                                  |
| auth-service   | ✅ UP  | port 8081                                                   |
| member-service | ✅ UP  | port 11000                                                  |
| MySQL          | ✅ UP  | localhost:3306, database `member`                           |
| Redis          | ✅ UP  | localhost:6379, started with `redis-server --daemonize yes` |
| RabbitMQ       | ❌ N/A | Not installed — domain event verification skipped           |

---

## Auth Endpoints (A1–A4)

### A1 — Register

| Item        | Result                                                                      |
| ----------- | --------------------------------------------------------------------------- |
| Endpoint    | `POST /auth/api/v1/register`                                                |
| HTTP code   | 200                                                                         |
| Response    | `code=200`, `success=true`, `userId=4006`                                   |
| DB check    | `member_member` row created: `status=active`, `gender=0`                    |
| Redis check | Refresh token key present: `pot:auth:auth:refresh:063b176f...` TTL=2591692s |

### A2 — Login

| Item        | Result                                                                  |
| ----------- | ----------------------------------------------------------------------- |
| Endpoint    | `POST /auth/api/v1/login`                                               |
| HTTP code   | 200                                                                     |
| Response    | `code=200`, `success=true`, new `accessToken` + `refreshToken` returned |
| Redis check | New refresh token key stored                                            |

### A3 — Refresh Token

| Item      | Result                                              |
| --------- | --------------------------------------------------- |
| Endpoint  | `POST /auth/api/v1/refresh`                         |
| HTTP code | 200                                                 |
| Response  | `code=200`, new `accessToken` differs from previous |

### A4 — Logout

| Item        | Result                                                        |
| ----------- | ------------------------------------------------------------- |
| Endpoint    | `POST /auth/api/v1/logout`                                    |
| HTTP code   | 200                                                           |
| Response    | `code=200`, `success=true`                                    |
| Redis check | Access token JTI `59741a4c-...` added to blacklist, TTL=3563s |

---

## Member Endpoints (M1–M7)

### M1 — Get Current Member

| Item      | Result                                                                   |
| --------- | ------------------------------------------------------------------------ |
| Endpoint  | `GET /api/members/me`                                                    |
| HTTP code | 200                                                                      |
| Response  | `code=200`, `memberId=4006`, `nickname=test_1775980539`, `status=ACTIVE` |
| Note      | Requires `X-User-Id` header (set by gateway filter)                      |

### M2 — Get Member by ID

| Item      | Result                                                                   |
| --------- | ------------------------------------------------------------------------ |
| Endpoint  | `GET /api/members/4006`                                                  |
| HTTP code | 200                                                                      |
| Response  | `code=200`, full member profile returned with `LocalDateTime` timestamps |

### M3 — Get Member Permissions

| Item      | Result                                                                 |
| --------- | ---------------------------------------------------------------------- |
| Endpoint  | `GET /api/members/4006/permissions`                                    |
| HTTP code | 200                                                                    |
| Response  | `code=200`, `data=[]` (no permissions assigned, expected for new user) |

### M4 — Update Profile

| Item      | Result                                                                    |
| --------- | ------------------------------------------------------------------------- |
| Endpoint  | `PUT /api/members/4006/profile`                                           |
| Body      | `{"firstName":"Test","lastName":"User","nickname":"test_updated_nick"}`   |
| HTTP code | 200                                                                       |
| Response  | `code=200`, `updatedAt` changed                                           |
| DB check  | `first_name=Test`, `last_name=User`, `gmt_updated_at` updated             |
| Note      | `nickname` update in profile not supported (nickname is login identifier) |

### M5 — Change Password

| Item      | Result                                                             |
| --------- | ------------------------------------------------------------------ |
| Endpoint  | `PUT /api/members/4006/password`                                   |
| Body      | `{"oldPassword":"Test@12345678","newPassword":"Test@NewPass9876"}` |
| HTTP code | 200                                                                |
| Response  | `code=200`, `success=true`                                         |
| DB check  | `password_hash` updated to new BCrypt hash `$2a$10$9gH3...`        |
| MQ check  | SKIPPED — RabbitMQ not installed                                   |

### M6 — Lock Member

| Item      | Result                                       |
| --------- | -------------------------------------------- |
| Endpoint  | `POST /api/members/4006/lock`                |
| HTTP code | 200                                          |
| Response  | `code=200`, `success=true`                   |
| DB check  | `status` set to `suspended` (domain: LOCKED) |
| MQ check  | SKIPPED — RabbitMQ not installed             |

### M7 — Unlock Member

| Item      | Result                           |
| --------- | -------------------------------- |
| Endpoint  | `POST /api/members/4006/unlock`  |
| HTTP code | 200                              |
| Response  | `code=200`, `success=true`       |
| DB check  | `status` restored to `active`    |
| MQ check  | SKIPPED — RabbitMQ not installed |

---

## Summary

| Test | Endpoint                          | Result  |
| ---- | --------------------------------- | ------- |
| A1   | POST /auth/api/v1/register        | ✅ PASS |
| A2   | POST /auth/api/v1/login           | ✅ PASS |
| A3   | POST /auth/api/v1/refresh         | ✅ PASS |
| A4   | POST /auth/api/v1/logout          | ✅ PASS |
| M1   | GET /api/members/me               | ✅ PASS |
| M2   | GET /api/members/{id}             | ✅ PASS |
| M3   | GET /api/members/{id}/permissions | ✅ PASS |
| M4   | PUT /api/members/{id}/profile     | ✅ PASS |
| M5   | PUT /api/members/{id}/password    | ✅ PASS |
| M6   | POST /api/members/{id}/lock       | ✅ PASS |
| M7   | POST /api/members/{id}/unlock     | ✅ PASS |

**All 11 tested endpoints PASSED.**

---

## Bugs Found and Fixed

| #   | Bug                                                                                 | Root Cause                                                                                                                                             | Fix                                                                     | Scope                |
| --- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------- | -------------------- |
| 1   | `R<T>` Feign deserialization silently fails                                         | `@Builder + @AllArgsConstructor(PRIVATE)` missing `@Jacksonized`                                                                                       | Added `@Jacksonized` to `R.java`                                        | framework-common     |
| 2   | `Member.Gender` getter/setter ambiguity → `Gender.valueOf("0")` NPE when reading DB | Lombok skips `setGender(Integer)` because manual `setGender(Gender)` exists                                                                            | Renamed manual setter to `setGenderEnum(Gender)`                        | member-service       |
| 3   | Null permissions NPE on register → `permissions.size()` throws                      | `findById` on new user returns `UserDTO` without permissions set                                                                                       | Added null-safe guard in `JwtTokenService.generateTokenPair()`          | auth-service         |
| 4   | Redis `Connection refused` in auth-service on register                              | Redis server not running                                                                                                                               | Started Redis with `redis-server --daemonize yes`                       | infra                |
| 5   | `GET /api/members/me` → 500 "Missing request attribute 'memberId'"                  | No filter converts gateway `X-User-Id` header to request attribute                                                                                     | Created `UserContextFilter` in member-service                           | member-service       |
| 6   | `LocalDateTime` serialization fails → 500 "Type definition error"                   | `MQAutoConfiguration.objectMapper()` registers plain `ObjectMapper` before Spring Boot's Jackson auto-config, preventing `JavaTimeModule` registration | Added `after = JacksonAutoConfiguration.class` to `MQAutoConfiguration` | framework-starter-mq |

---

## Known Gaps

- **RabbitMQ domain events** (M5 password changed, M6 locked, M7 unlocked): Cannot verify because RabbitMQ is not installed in this test environment. Service logged operations succeed; events were attempted but delivery unverifiable.
- **M4 nickname update**: The `updateProfile` use case intentionally does not support nickname changes because nickname is used as the login identifier. Passing `nickname` in the body is silently ignored.
- **Actuator health endpoint**: Member-service health check returns 500 because Spring Boot Actuator is not configured. Service functions correctly for all business endpoints.
