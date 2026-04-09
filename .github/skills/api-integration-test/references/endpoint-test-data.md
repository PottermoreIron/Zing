# 接口测试数据参考

占位符说明：

- `{NICKNAME}` — 生成的唯一昵称，如 `test_853421`
- `{EMAIL}` — 生成的唯一邮箱，如 `test_853421@zing.test`
- `{PASSWORD}` — 初始密码 `Test@12345678`
- `{NEW_PASSWORD}` — 修改后密码 `Test@NewPass9876`
- `{USER_DOMAIN}` — 用户域 `USER`
- `{MEMBER_ID}` — 注册后获取的 userId（Long 类型）
- `{ACCESS_TOKEN}` — 当前有效的 JWT accessToken
- `{REFRESH_TOKEN}` — 当前有效的 JWT refreshToken

---

## Auth 模块（auth-service: http://localhost:8081）

---

### A1 — 注册

| 字段        | 值                 |
| ----------- | ------------------ |
| 方法        | `POST`             |
| 路径        | `/api/v1/register` |
| operationId | `authRegister`     |
| 需要认证    | 否                 |

**请求体：**

```json
{
  "registerType": "USERNAME_PASSWORD",
  "nickname": "{NICKNAME}",
  "password": "{PASSWORD}",
  "userDomain": "{USER_DOMAIN}"
}
```

**预期响应：**

```json
{
  "code": 0,
  "data": {
    "userId": 123456789,
    "userDomain": "USER",
    "nickname": "{NICKNAME}",
    "email": null,
    "phoneNumber": null,
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "accessTokenExpiresAt": 1744200000000,
    "refreshTokenExpiresAt": 1744800000000,
    "message": null
  }
}
```

**断言字段：**

- `code == 0`
- `data.userId` 不为 null（记录到 `MEMBER_ID`）
- `data.nickname == "{NICKNAME}"`
- `data.accessToken` 长度 > 100
- `data.refreshToken` 长度 > 100

**错误场景：**

- `400 / code != 0`：昵称格式不合法（昵称要求：字母数字下划线连字符，2-32位）
- `409 / code != 0`：昵称已存在

---

### A2 — 登录（用户名密码）

| 字段        | 值              |
| ----------- | --------------- |
| 方法        | `POST`          |
| 路径        | `/api/v1/login` |
| operationId | `authLogin`     |
| 需要认证    | 否              |
| 限流        | 5 req/IP/min    |

**请求体：**

```json
{
  "loginType": "USERNAME_PASSWORD",
  "nickname": "{NICKNAME}",
  "password": "{PASSWORD}",
  "userDomain": "{USER_DOMAIN}"
}
```

**预期响应：**

```json
{
  "code": 0,
  "data": {
    "userId": "{MEMBER_ID}",
    "userDomain": "USER",
    "nickname": "{NICKNAME}",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

**断言字段：**

- `code == 0`
- `data.userId == {MEMBER_ID}`
- `data.accessToken` 不为 null

**错误场景：**

- `401 / code != 0`：密码错误
- `429`：触发限流

---

### A3 — Token 刷新

| 字段        | 值                      |
| ----------- | ----------------------- |
| 方法        | `POST`                  |
| 路径        | `/api/v1/refresh`       |
| operationId | `authRefreshToken`      |
| 需要认证    | 否（使用 refreshToken） |
| 限流        | 10 req/IP/min           |

**请求体：**

```json
{
  "refreshToken": "{REFRESH_TOKEN}"
}
```

**预期响应：**

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

**断言字段：**

- `code == 0`
- `data.accessToken` 与入参 `{ACCESS_TOKEN}` 不同（新 token）

---

### A4 — 登出

| 字段        | 值                                           |
| ----------- | -------------------------------------------- |
| 方法        | `POST`                                       |
| 路径        | `/api/v1/logout`                             |
| operationId | `authLogout`                                 |
| 需要认证    | 是（`Authorization: Bearer {ACCESS_TOKEN}`） |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**请求体（可选）：**

```json
{
  "refreshToken": "{REFRESH_TOKEN}"
}
```

**预期响应：**

```json
{
  "code": 0,
  "data": null
}
```

**断言字段：**

- `code == 0`
- 使用已登出的 token 再调用任意需认证接口，期望返回 `401`

---

## Member 模块（member-service: http://localhost:11000）

---

### M1 — 获取当前会员

| 字段     | 值                |
| -------- | ----------------- |
| 方法     | `GET`             |
| 路径     | `/api/members/me` |
| 需要认证 | 是                |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**预期响应：**

```json
{
  "code": 0,
  "data": {
    "memberId": "{MEMBER_ID}",
    "nickname": "{NICKNAME}",
    "email": null,
    "status": "ACTIVE"
  }
}
```

**断言字段：**

- `code == 0`
- `data.memberId == {MEMBER_ID}`
- `data.nickname == "{NICKNAME}"`
- `data.status == "ACTIVE"`

---

### M2 — 按 ID 查询会员

| 字段     | 值                         |
| -------- | -------------------------- |
| 方法     | `GET`                      |
| 路径     | `/api/members/{MEMBER_ID}` |
| 需要认证 | 是                         |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**预期响应：**

```json
{
  "code": 0,
  "data": {
    "memberId": "{MEMBER_ID}",
    "nickname": "{NICKNAME}"
  }
}
```

**断言字段：**

- `code == 0`
- `data.memberId == {MEMBER_ID}`

---

### M3 — 获取权限列表

| 字段     | 值                                     |
| -------- | -------------------------------------- |
| 方法     | `GET`                                  |
| 路径     | `/api/members/{MEMBER_ID}/permissions` |
| 需要认证 | 是                                     |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**预期响应：**

```json
{
  "code": 0,
  "data": []
}
```

**断言字段：**

- `code == 0`
- `data` 为数组（可为空，新用户无额外权限）

---

### M4 — 更新会员资料

| 字段     | 值                                 |
| -------- | ---------------------------------- |
| 方法     | `PUT`                              |
| 路径     | `/api/members/{MEMBER_ID}/profile` |
| 需要认证 | 是                                 |

**请求体：**

```json
{
  "nickname": "{NICKNAME}_upd",
  "firstName": "Test",
  "lastName": "User",
  "gender": 1,
  "bio": "Integration test bio",
  "countryCode": "CN",
  "timezone": "Asia/Shanghai",
  "locale": "zh_CN"
}
```

> 注意：更新后的 nickname 中 `_upd` 后缀总长 ≤ 32 字符。若生成的 NICKNAME 已接近 32 字符，省略 `nickname` 字段。

**预期响应：**

```json
{
  "code": 0
}
```

**断言字段：**

- `code == 0`

---

### M5 — 修改密码

| 字段     | 值                                  |
| -------- | ----------------------------------- |
| 方法     | `PUT`                               |
| 路径     | `/api/members/{MEMBER_ID}/password` |
| 需要认证 | 是                                  |

**请求体：**

```json
{
  "oldPassword": "{PASSWORD}",
  "newPassword": "{NEW_PASSWORD}"
}
```

**预期响应：**

```json
{
  "code": 0
}
```

**断言字段：**

- `code == 0`
- 使用旧密码登录期望失败（401）
- 使用新密码登录期望成功

---

### M6 — 锁定账户

| 字段     | 值                              |
| -------- | ------------------------------- |
| 方法     | `POST`                          |
| 路径     | `/api/members/{MEMBER_ID}/lock` |
| 需要认证 | 是                              |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**请求体：** 无

**预期响应：**

```json
{
  "code": 0
}
```

**断言字段：**

- `code == 0`
- DB 中 `status` 变为 `LOCKED`

---

### M7 — 解锁账户

| 字段     | 值                                |
| -------- | --------------------------------- |
| 方法     | `POST`                            |
| 路径     | `/api/members/{MEMBER_ID}/unlock` |
| 需要认证 | 是                                |

**请求头：**

```
Authorization: Bearer {ACCESS_TOKEN}
```

**请求体：** 无

**预期响应：**

```json
{
  "code": 0
}
```

**断言字段：**

- `code == 0`
- DB 中 `status` 变回 `ACTIVE`

---

## 测试执行顺序（依赖关系）

```
A1(注册) → A2(登录) → A3(刷新) → A4(登出) → [重新登录]
                                                    ↓
                                    M1(getMe) → M2(getById) → M3(permissions)
                                                    ↓
                                    M4(profile) → M5(password) → M6(lock) → M7(unlock)
```

M6(lock) **必须**在 M7(unlock) 之前执行，避免账户被永久锁定。
