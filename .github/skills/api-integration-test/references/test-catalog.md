# Test Catalog

Per-endpoint test data: request, assertions, and data-layer verification.

## Placeholders

| Var               | Source                                     |
| ----------------- | ------------------------------------------ |
| `{NICKNAME}`      | Generated `test_{TS}`                      |
| `{PASSWORD}`      | `Test@12345678`                            |
| `{NEW_PASSWORD}`  | `Test@NewPass9876`                         |
| `{USER_DOMAIN}`   | `USER`                                     |
| `{MEMBER_ID}`     | From register/login response `data.userId` |
| `{ACCESS_TOKEN}`  | Current valid JWT                          |
| `{REFRESH_TOKEN}` | Current valid refresh JWT                  |
| `{TOKEN_ID}`      | JWT payload `jti` field                    |

---

## Auth Module — `{AUTH_BASE}` (default `http://localhost:8081`)

### A1 — POST /api/v1/register

**Auth:** No | **Rate limit:** None

**Request:**

```json
{
  "registerType": "USERNAME_PASSWORD",
  "nickname": "{NICKNAME}",
  "password": "{PASSWORD}",
  "userDomain": "{USER_DOMAIN}"
}
```

**Assertions:** `code==0`, `data.userId` not null → save as `MEMBER_ID`, `data.accessToken` length > 100, `data.nickname == {NICKNAME}`

**Extract:** `MEMBER_ID=data.userId`, `ACCESS_TOKEN=data.accessToken`, `REFRESH_TOKEN=data.refreshToken`

**DB verification:**

```sql
SELECT member_id, nickname, status, gmt_create
FROM member.member_member WHERE nickname = '{NICKNAME}' LIMIT 1;
-- Expect: 1 row, status='ACTIVE', member_id not null
```

---

### A2 — POST /api/v1/login

**Auth:** No | **Rate limit:** 5 req/IP/min (add 1s delay before call)

**Request:**

```json
{
  "loginType": "USERNAME_PASSWORD",
  "nickname": "{NICKNAME}",
  "password": "{PASSWORD}",
  "userDomain": "{USER_DOMAIN}"
}
```

**Assertions:** `code==0`, `data.userId == {MEMBER_ID}`, `data.accessToken` not null

**Extract:** `ACCESS_TOKEN=data.accessToken`, `REFRESH_TOKEN=data.refreshToken`. Parse JWT payload middle segment (base64) to get `TOKEN_ID=jti`.

**Redis verification:**

```
GET auth:blacklist:{TOKEN_ID}
-- Expect: nil (token not blacklisted)
```

---

### A3 — POST /api/v1/refresh

**Auth:** No (uses refresh token) | **Rate limit:** 10 req/IP/min

**Request:**

```json
{ "refreshToken": "{REFRESH_TOKEN}" }
```

**Assertions:** `code==0`, `data.accessToken` differs from previous `ACCESS_TOKEN`

**Extract:** `ACCESS_TOKEN=data.accessToken`, `REFRESH_TOKEN=data.refreshToken`

---

### A4 — POST /api/v1/logout

**Auth:** `Bearer {ACCESS_TOKEN}`

**Request:**

```json
{ "refreshToken": "{REFRESH_TOKEN}" }
```

**Assertions:** `code==0`

**Redis verification:**

```
GET auth:blacklist:{TOKEN_ID}
-- Expect: not nil (token blacklisted)
-- Also check: TTL auth:blacklist:{TOKEN_ID} → positive (has expiry)
```

**Post-step:** Re-login to obtain fresh tokens for subsequent member tests (reuse A2 flow).

---

## Member Module — `{MEMBER_BASE}` (default `http://localhost:11000`)

### M1 — GET /api/members/me

**Auth:** `Bearer {ACCESS_TOKEN}`

**Assertions:** `code==0`, `data.memberId == {MEMBER_ID}`, `data.nickname == {NICKNAME}`, `data.status == "ACTIVE"`

---

### M2 — GET /api/members/{MEMBER_ID}

**Auth:** `Bearer {ACCESS_TOKEN}`

**Assertions:** `code==0`, `data.memberId == {MEMBER_ID}`

---

### M3 — GET /api/members/{MEMBER_ID}/permissions

**Auth:** `Bearer {ACCESS_TOKEN}`

**Assertions:** `code==0`, `data` is array (may be empty for new user)

**Redis verification:**

```
GET auth:perm:version:{USER_DOMAIN}:{MEMBER_ID}
-- Expect: version string or nil (nil is OK for new user → [INFO])
```

---

### M4 — PUT /api/members/{MEMBER_ID}/profile

**Auth:** `Bearer {ACCESS_TOKEN}`

**Request:**

```json
{
  "nickname": "{NICKNAME}_updated",
  "firstName": "Test",
  "lastName": "User",
  "gender": 1,
  "bio": "Integration test bio",
  "countryCode": "CN",
  "timezone": "Asia/Shanghai",
  "locale": "zh_CN"
}
```

**Assertions:** `code==0`

**DB verification:**

```sql
SELECT nickname, first_name, last_name, gender, bio, country_code, timezone, locale
FROM member.member_member WHERE member_id = {MEMBER_ID};
-- Expect: nickname='{NICKNAME}_updated', first_name='Test', gender=1
```

**MQ verification:** Check for `MemberProfileUpdated` event — peek member-related queues for `memberId == {MEMBER_ID}`.

---

### M5 — PUT /api/members/{MEMBER_ID}/password

**Auth:** `Bearer {ACCESS_TOKEN}`

**Pre-step:** Record old password hash:

```sql
SELECT password_hash FROM member.member_member WHERE member_id = {MEMBER_ID};
```

**Request:**

```json
{ "oldPassword": "{PASSWORD}", "newPassword": "{NEW_PASSWORD}" }
```

**Assertions:** `code==0`

**DB verification:**

```sql
SELECT password_hash FROM member.member_member WHERE member_id = {MEMBER_ID};
-- Expect: differs from pre-step value
```

**Post-step:** Update `PASSWORD={NEW_PASSWORD}` for subsequent tests.

**MQ verification:** Check for `MemberPasswordChanged` event.

---

### M6 — POST /api/members/{MEMBER_ID}/lock

**Auth:** `Bearer {ACCESS_TOKEN}`

**Assertions:** `code==0`

**DB verification:**

```sql
SELECT status FROM member.member_member WHERE member_id = {MEMBER_ID};
-- Expect: status='LOCKED'
```

**MQ verification:** Check for `MemberLocked` event.

---

### M7 — POST /api/members/{MEMBER_ID}/unlock

**Auth:** `Bearer {ACCESS_TOKEN}`

**Assertions:** `code==0`

**DB verification:**

```sql
SELECT status FROM member.member_member WHERE member_id = {MEMBER_ID};
-- Expect: status='ACTIVE'
```

**MQ verification:** Check for `MemberUnlocked` event.

---

## MQ Discovery Protocol

RabbitMQ queue/exchange names depend on service config. Use this process:

1. `list_queues` → find queues matching `member` or `event`.
2. After each write operation, `peek` the relevant queue (count=1, ack=false).
3. Verify payload contains `memberId == {MEMBER_ID}` and matching event type.
4. If no member-related queues exist → `[INFO - no domain events found, verify RabbitMQ config]`.
