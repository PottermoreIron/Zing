# 账户绑定服务重构技术规范

## 文档信息

- **项目**: Zing - 账户绑定服务架构重构
- **版本**: v1.0
- **日期**: 2025年11月4日
- **作者**: AI架构师
- **状态**: 设计方案 - 待实施

---

## 目录

1. [现状分析](#现状分析)
2. [问题识别](#问题识别)
3. [架构设计原则](#架构设计原则)
4. [解决方案设计](#解决方案设计)
5. [详细设计](#详细设计)
6. [API设计](#api设计)
7. [数据流设计](#数据流设计)
8. [实施计划](#实施计划)

---

## 现状分析

### 当前架构

```
auth-service/
└── controller/v1/
    └── AccountBindingController.java
        └── AccountBindingService
            └── AccountBindingServiceImpl (直接操作数据，模拟实现)

member-service/
└── service/
    └── SocialConnectionsService (仅MyBatis Plus基础接口)
        └── SocialConnectionsServiceImpl (空实现)
    └── entity/
        └── SocialConnection (完整的实体定义)

member-facade/
└── api/
    └── MemberFacade (有OAuth2相关接口，但未被使用)
```

### 现有代码特征

#### AccountBindingServiceImpl 问题

```java
// ❌ 问题1: 直接在Auth服务中操作Member领域数据
// 注释掉的代码显示原计划是操作 accountBindingMapper
// private final AccountBindingMapper accountBindingMapper;

// ❌ 问题2: 模拟实现，没有真实的数据库操作
String openId = "simulated_open_id_" + System.currentTimeMillis();

// ❌ 问题3: 业务逻辑分散，缺乏统一管理
// 绑定、解绑、查询、刷新等逻辑都在一个类中
```

#### SocialConnection 优势

```java
// ✅ 优势1: 完整的实体定义
@TableName("member_social_connection")
public class SocialConnection {
    private Long memberId;           // 关联用户ID
    private String provider;         // 第三方平台
    private String providerMemberId; // 第三方用户ID
    private String accessToken;      // 访问令牌
    private String refreshToken;     // 刷新令牌
    // ... 完整的字段定义
    
// ✅ 优势2: 丰富的业务方法
    public boolean isActive() { ... }
    public boolean isTokenExpired() { ... }
    public boolean isValidConnection() { ... }
}
```

---

## 问题识别

### 1. 架构层次问题

**违反分层架构原则**

- Auth服务不应直接访问Member服务的数据表
- 跨域数据访问应通过Facade接口
- 缺乏清晰的服务边界

### 2. 领域职责混乱

**Auth服务职责**

- ✅ 认证（Authentication）
- ✅ 授权（Authorization）
- ✅ Token管理
- ❌ 第三方账号绑定（应属于Member领域）

**Member服务职责**

- ✅ 用户信息管理
- ✅ 社交账号连接管理（SocialConnection）
- ❌ 未提供完整的Facade接口供其他服务调用

### 3. 数据一致性风险

```
场景: 用户绑定微信账号
当前实现: Auth服务直接操作member_social_connection表
风险: 
  - 绕过Member服务的业务逻辑
  - 数据验证不完整
  - 无法触发Member服务的事件通知
  - 分布式事务问题
```

### 4. 扩展性问题

```
新增需求: 支持多个微信账号绑定（个人微信 + 企业微信）
当前架构: 需要修改Auth服务代码
理想架构: 只需要修改Member服务，Auth服务无感知
```

---

## 架构设计原则

### 1. 领域驱动设计 (DDD)

```
Auth领域: 
  - 认证上下文 (Authentication Context)
  - 职责: 验证用户身份，颁发令牌

Member领域:
  - 会员上下文 (Member Context)
  - 职责: 管理用户信息，包括社交账号连接
```

### 2. 微服务设计原则

**服务自治**: 每个服务管理自己的数据

```
Auth服务 ──RPC调用──> Member服务
  ↓                      ↓
Auth数据库            Member数据库
(tokens, sessions)    (members, social_connections)
```

**接口契约**: 通过Facade定义清晰的服务接口

```java
// ✅ 正确的跨服务调用方式
memberFacade.bindSocialAccount(memberId, provider, socialInfo);

// ❌ 错误的方式（直接访问数据库）
socialConnectionMapper.insert(connection);
```

### 3. 单一职责原则 (SRP)

```
AccountBindingController:
  - 职责: HTTP请求处理、参数验证、响应封装
  - 不负责: 业务逻辑、数据访问

AccountBindingService:
  - 职责: 编排业务流程、调用Facade接口
  - 不负责: 直接数据访问

MemberFacade:
  - 职责: 服务间接口定义
  - 不负责: 具体实现

SocialConnectionsService:
  - 职责: 社交账号连接的业务逻辑和数据访问
  - 所有者: Member服务
```

### 4. 开闭原则 (OCP)

```
扩展场景: 新增Facebook登录
需要修改的地方:
  ✅ Member服务: 新增Provider枚举值
  ✅ OAuth2配置: 新增Facebook配置
  ❌ Auth服务核心逻辑: 无需修改
```

---

## 解决方案设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端 (Client)                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ HTTP Request
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Gateway (API网关)                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ Route
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Auth Service (认证服务)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   AccountBindingController                           │   │
│  │   - bindAccount()                                    │   │
│  │   - unbindAccount()                                  │   │
│  │   - listBindings()                                   │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
│                    │ 调用                                     │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   AccountBindingService                              │   │
│  │   - 业务流程编排                                       │   │
│  │   - 参数验证                                           │   │
│  │   - 调用MemberFacade                                  │   │
│  │   - 调用OAuth2Service (获取用户信息)                   │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
└────────────────────┼─────────────────────────────────────────┘
                     │
                     │ RPC调用 (Feign)
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Member Service (会员服务)                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   MemberFacade (接口实现)                             │   │
│  │   - bindSocialAccount()                              │   │
│  │   - unbindSocialAccount()                            │   │
│  │   - getSocialConnections()                           │   │
│  │   - getSocialConnection()                            │   │
│  │   - updateSocialConnection()                         │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
│                    │ 调用                                     │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   SocialConnectionsService                           │   │
│  │   - createConnection()                               │   │
│  │   - removeConnection()                               │   │
│  │   - queryConnections()                               │   │
│  │   - validateConnection()                             │   │
│  │   - updateTokens()                                   │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
│                    │ 持久化                                   │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │   SocialConnectionsMapper (MyBatis)                  │   │
│  └─────────────────┬────────────────────────────────────┘   │
│                    │                                         │
└────────────────────┼─────────────────────────────────────────┘
                     │
                     ▼
              ┌─────────────┐
              │   Database  │
              │  (MySQL)    │
              └─────────────┘
```

### 数据流设计

#### 场景1: 用户绑定微信账号

```
1. 用户点击"绑定微信" → 前端跳转到微信授权页
   
2. 微信回调 → Gateway → Auth Service
   POST /api/v1/auth/bindings
   {
     "provider": "wechat",
     "code": "wx_auth_code_xxx",
     "state": "csrf_token"
   }

3. Auth Service处理:
   ┌─────────────────────────────────────────┐
   │ AccountBindingController                │
   │  ↓                                      │
   │ AccountBindingService                   │
   │  ├─ 验证state (CSRF防护)                │
   │  ├─ 调用OAuth2Service获取微信用户信息   │
   │  │  → WeChatOAuth2Service.getUserInfo() │
   │  │  → 返回: openId, nickname, avatar    │
   │  │                                       │
   │  └─ 调用MemberFacade                     │
   └────────┼───────────────────────────────┘
            │
            │ RPC: memberFacade.bindSocialAccount()
            │ 参数: {
            │   memberId: 10001,
            │   provider: "wechat",
            │   providerMemberId: "wx_openid_xxx",
            │   providerUsername: "张三",
            │   accessToken: "access_token_xxx",
            │   refreshToken: "refresh_token_xxx",
            │   expiresAt: 1699999999,
            │   scope: "snsapi_userinfo",
            │   extendJson: "{...}"
            │ }
            ▼
   ┌─────────────────────────────────────────┐
   │ Member Service                          │
   │  ├─ MemberFacadeImpl.bindSocialAccount()│
   │  │   ↓                                  │
   │  ├─ SocialConnectionsService            │
   │  │   ├─ 验证用户是否存在                │
   │  │   ├─ 检查是否已绑定该平台            │
   │  │   ├─ 检查openId是否被其他用户绑定    │
   │  │   ├─ 创建SocialConnection实体       │
   │  │   ├─ 保存到数据库                    │
   │  │   └─ 返回绑定信息                    │
   │  │                                      │
   │  └─ 返回结果给Auth Service              │
   └─────────────────────────────────────────┘

4. Auth Service返回结果给前端:
   {
     "code": 200,
     "message": "绑定成功",
     "data": {
       "bindingId": 123,
       "provider": "wechat",
       "providerUsername": "张三",
       "avatarUrl": "https://...",
       "boundAt": 1699999999
     }
   }
```

#### 场景2: 用户解绑账号

```
DELETE /api/v1/auth/bindings/wechat

Auth Service:
  └─ AccountBindingService
      ├─ 获取当前用户ID
      ├─ 检查是否至少保留一种登录方式
      └─ 调用 memberFacade.unbindSocialAccount(memberId, "wechat")

Member Service:
  └─ SocialConnectionsService
      ├─ 查询绑定记录
      ├─ 验证绑定是否存在
      ├─ 软删除记录 (设置gmtDeletedAt)
      └─ 返回成功
```

---

## 详细设计

### 1. Member Facade 接口扩展

#### 1.1 新增DTO定义

**SocialConnectionDTO.java** (新建)

```java
package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 社交账号连接信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialConnectionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 连接ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long memberId;

    /**
     * 第三方平台提供商
     */
    private String provider;

    /**
     * 第三方平台用户ID
     */
    private String providerMemberId;

    /**
     * 第三方平台用户名
     */
    private String providerUsername;

    /**
     * 第三方平台邮箱
     */
    private String providerEmail;

    /**
     * 头像URL（从扩展信息中提取）
     */
    private String avatarUrl;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 绑定时间
     */
    private Long boundAt;

    /**
     * 更新时间
     */
    private Long updatedAt;

    /**
     * 最后使用时间
     */
    private Long lastUsedAt;

    /**
     * 是否为主账号
     */
    private Boolean isPrimary;
}
```

**BindSocialAccountRequest.java** (新建)

```java
package com.pot.member.facade.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 绑定社交账号请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindSocialAccountRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long memberId;

    /**
     * 第三方平台提供商
     */
    @NotBlank(message = "平台提供商不能为空")
    private String provider;

    /**
     * 第三方平台用户ID
     */
    @NotBlank(message = "第三方平台用户ID不能为空")
    private String providerMemberId;

    /**
     * 第三方平台用户名
     */
    private String providerUsername;

    /**
     * 第三方平台邮箱
     */
    private String providerEmail;

    /**
     * 访问令牌
     */
    @NotBlank(message = "访问令牌不能为空")
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌过期时间
     */
    private Long tokenExpiresAt;

    /**
     * 授权范围
     */
    private String scope;

    /**
     * 扩展信息 (JSON格式)
     */
    private String extendJson;
}
```

#### 1.2 MemberFacade 接口扩展

```java
package com.pot.member.facade.api;

import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "member-service", path = "/member")
public interface MemberFacade {
    
    // ... 现有方法保持不变 ...

    /**
     * 绑定社交账号
     * 
     * @param request 绑定请求
     * @return 绑定后的连接信息
     */
    @PostMapping("/social-connections/bind")
    R<SocialConnectionDTO> bindSocialAccount(@RequestBody BindSocialAccountRequest request);

    /**
     * 解绑社交账号
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @DeleteMapping("/social-connections/unbind")
    R<Void> unbindSocialAccount(@RequestParam("memberId") Long memberId,
                                @RequestParam("provider") String provider);

    /**
     * 获取用户的所有社交账号连接
     * 
     * @param memberId 用户ID
     * @return 连接列表
     */
    @GetMapping("/social-connections/list")
    R<List<SocialConnectionDTO>> getSocialConnections(@RequestParam("memberId") Long memberId);

    /**
     * 获取特定平台的连接信息
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 连接信息
     */
    @GetMapping("/social-connections/get")
    R<SocialConnectionDTO> getSocialConnection(@RequestParam("memberId") Long memberId,
                                               @RequestParam("provider") String provider);

    /**
     * 检查第三方账号是否已被绑定
     * 
     * @param provider 平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return true-已绑定，false-未绑定
     */
    @GetMapping("/social-connections/check-bound")
    R<Boolean> isSocialAccountBound(@RequestParam("provider") String provider,
                                    @RequestParam("providerMemberId") String providerMemberId);

    /**
     * 根据第三方账号查询用户ID
     * 
     * @param provider 平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return 用户ID，未找到返回null
     */
    @GetMapping("/social-connections/get-member-id")
    R<Long> getMemberIdBySocialAccount(@RequestParam("provider") String provider,
                                       @RequestParam("providerMemberId") String providerMemberId);

    /**
     * 更新社交账号令牌
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @param accessToken 新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresAt 过期时间
     * @return 操作结果
     */
    @PutMapping("/social-connections/update-tokens")
    R<Void> updateSocialAccountTokens(@RequestParam("memberId") Long memberId,
                                      @RequestParam("provider") String provider,
                                      @RequestParam("accessToken") String accessToken,
                                      @RequestParam(value = "refreshToken", required = false) String refreshToken,
                                      @RequestParam(value = "expiresAt", required = false) Long expiresAt);

    /**
     * 设置主账号
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @PutMapping("/social-connections/set-primary")
    R<Void> setPrimarySocialAccount(@RequestParam("memberId") Long memberId,
                                    @RequestParam("provider") String provider);
}
```

### 2. Member Service 实现

#### 2.1 SocialConnectionsService 业务方法

```java
package com.pot.member.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pot.member.service.entity.SocialConnection;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import java.util.List;

/**
 * 社交账号连接服务
 */
public interface SocialConnectionsService extends IService<SocialConnection> {

    /**
     * 创建社交账号连接
     * 
     * @param request 绑定请求
     * @return 创建的连接
     */
    SocialConnection createConnection(BindSocialAccountRequest request);

    /**
     * 删除社交账号连接
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void removeConnection(Long memberId, String provider);

    /**
     * 查询用户的所有连接
     * 
     * @param memberId 用户ID
     * @return 连接列表
     */
    List<SocialConnection> listByMemberId(Long memberId);

    /**
     * 查询特定平台的连接
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 连接信息
     */
    SocialConnection getByMemberIdAndProvider(Long memberId, String provider);

    /**
     * 根据第三方账号查询连接
     * 
     * @param provider 平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return 连接信息
     */
    SocialConnection getByProviderAndProviderId(String provider, String providerMemberId);

    /**
     * 更新令牌信息
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresAt 过期时间
     */
    void updateTokens(Long memberId, String provider, String accessToken, 
                     String refreshToken, Long expiresAt);

    /**
     * 设置主账号
     * 
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void setPrimary(Long memberId, String provider);

    /**
     * 检查用户是否可以解绑（至少保留一种登录方式）
     * 
     * @param memberId 用户ID
     * @return true-可以解绑，false-不可以
     */
    boolean canUnbind(Long memberId);

    /**
     * 验证绑定请求
     * 
     * @param request 绑定请求
     */
    void validateBindRequest(BindSocialAccountRequest request);
}
```

### 3. Auth Service 重构

#### 3.1 AccountBindingService 重构

```java
package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.request.BindAccountRequest;
import com.pot.auth.service.dto.request.UnbindAccountRequest;
import com.pot.auth.service.dto.response.AccountBindingInfo;
import java.util.List;

/**
 * 账户绑定服务
 * 
 * 职责:
 * 1. 编排OAuth2授权流程和Member服务调用
 * 2. 转换DTO对象
 * 3. 业务流程控制
 * 
 * 不负责:
 * - 直接数据库操作（由Member服务负责）
 * - 具体的OAuth2实现细节（由OAuth2Service负责）
 */
public interface AccountBindingService {

    /**
     * 绑定第三方账号
     * 
     * 流程:
     * 1. 验证state (CSRF)
     * 2. 使用code换取OAuth2用户信息
     * 3. 调用MemberFacade创建绑定
     * 
     * @param userId 当前用户ID
     * @param request 绑定请求
     * @return 绑定信息
     */
    AccountBindingInfo bindAccount(Long userId, BindAccountRequest request);

    /**
     * 解绑第三方账号
     * 
     * @param userId 当前用户ID
     * @param request 解绑请求
     */
    void unbindAccount(Long userId, UnbindAccountRequest request);

    /**
     * 获取用户的所有绑定
     * 
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<AccountBindingInfo> listBindings(Long userId);

    /**
     * 获取特定平台的绑定信息
     * 
     * @param userId 用户ID
     * @param provider 平台提供商
     * @return 绑定信息
     */
    AccountBindingInfo getBinding(Long userId, String provider);

    /**
     * 设置主账号
     * 
     * @param userId 用户ID
     * @param provider 平台提供商
     */
    void setPrimaryAccount(Long userId, String provider);

    /**
     * 检查第三方账号是否已被绑定
     * 
     * @param provider 平台提供商
     * @param openId 第三方用户ID
     * @return true-已绑定，false-未绑定
     */
    boolean isAccountBound(String provider, String openId);

    /**
     * 刷新绑定信息
     * 
     * @param userId 用户ID
     * @param provider 平台提供商
     * @return 更新后的绑定信息
     */
    AccountBindingInfo refreshBinding(Long userId, String provider);
}
```

---

## API设计

### Auth Service API

```yaml
# 绑定第三方账号
POST /api/v1/auth/bindings
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "provider": "wechat",
  "code": "wx_auth_code_xxx",
  "state": "csrf_token_xxx"
}

Response:
{
  "code": 200,
  "message": "绑定成功",
  "data": {
    "bindingId": 123,
    "provider": "wechat",
    "openId": "wx_o***id",
    "nickname": "张三",
    "avatarUrl": "https://...",
    "isPrimary": false,
    "status": "ACTIVE",
    "boundAt": 1699999999,
    "lastUsedAt": 1699999999
  }
}

---

# 解绑第三方账号
DELETE /api/v1/auth/bindings/{provider}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "解绑成功"
}

---

# 获取绑定列表
GET /api/v1/auth/bindings
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "bindingId": 123,
      "provider": "wechat",
      "nickname": "张三",
      "avatarUrl": "https://...",
      "isPrimary": true,
      "boundAt": 1699999999
    },
    {
      "bindingId": 124,
      "provider": "github",
      "nickname": "zhangsan",
      "avatarUrl": "https://...",
      "isPrimary": false,
      "boundAt": 1699999990
    }
  ]
}
```

### Member Service Internal API (通过Facade)

```yaml
# 绑定社交账号 (内部RPC)
POST /member/social-connections/bind
Content-Type: application/json

Request:
{
  "memberId": 10001,
  "provider": "wechat",
  "providerMemberId": "wx_openid_xxx",
  "providerUsername": "张三",
  "providerEmail": null,
  "accessToken": "access_token_xxx",
  "refreshToken": "refresh_token_xxx",
  "tokenExpiresAt": 1699999999,
  "scope": "snsapi_userinfo",
  "extendJson": "{\"avatar\":\"https://...\"}"
}

Response:
{
  "code": 200,
  "message": "绑定成功",
  "data": {
    "id": 123,
    "memberId": 10001,
    "provider": "wechat",
    "providerMemberId": "wx_openid_xxx",
    "providerUsername": "张三",
    "avatarUrl": "https://...",
    "isActive": true,
    "boundAt": 1699999999
  }
}
```

---

## 实施计划

### Phase 1: Member Service 实现 (优先级: 高)

**任务列表:**

1. ✅ 创建DTO类
   - [ ] SocialConnectionDTO
   - [ ] BindSocialAccountRequest
   
2. ✅ 扩展MemberFacade接口
   - [ ] 新增社交账号相关方法
   
3. ✅ 实现SocialConnectionsService
   - [ ] 业务逻辑方法
   - [ ] 数据验证
   - [ ] 数据访问
   
4. ✅ 实现MemberFacadeImpl
   - [ ] Facade接口实现
   - [ ] DTO转换
   
5. ✅ 单元测试
   - [ ] Service层测试
   - [ ] Facade层测试

**预计时间:** 2-3天

### Phase 2: Auth Service 重构 (优先级: 高)

**任务列表:**

1. ✅ 重构AccountBindingServiceImpl
   - [ ] 移除直接数据库操作
   - [ ] 集成MemberFacade调用
   - [ ] 集成OAuth2Service
   
2. ✅ 更新AccountBindingController
   - [ ] 调整返回数据结构
   - [ ] 异常处理
   
3. ✅ 集成测试
   - [ ] 绑定流程测试
   - [ ] 解绑流程测试
   - [ ] 查询流程测试

**预计时间:** 1-2天

### Phase 3: 测试与优化 (优先级: 中)

**任务列表:**

1. ✅ 端到端测试
   - [ ] 完整的绑定流程
   - [ ] 完整的解绑流程
   - [ ] 异常场景测试
   
2. ✅ 性能优化
   - [ ] RPC调用优化
   - [ ] 缓存策略
   
3. ✅ 文档更新
   - [ ] API文档
   - [ ] 架构文档

**预计时间:** 1天

---

## 技术风险与应对

### 风险1: RPC调用失败

**场景**: Member服务不可用时的降级处理

**应对方案:**

```java
@FeignClient(
    name = "member-service",
    path = "/member",
    fallbackFactory = MemberFacadeFallbackFactory.class
)
public interface MemberFacade {
    // ... 接口定义
}

@Component
public class MemberFacadeFallbackFactory implements FallbackFactory<MemberFacade> {
    @Override
    public MemberFacade create(Throwable cause) {
        return new MemberFacade() {
            @Override
            public R<SocialConnectionDTO> bindSocialAccount(BindSocialAccountRequest request) {
                log.error("Member服务调用失败，进入降级处理", cause);
                return R.fail("服务暂时不可用，请稍后重试");
            }
            // ... 其他方法的降级实现
        };
    }
}
```

### 风险2: 数据一致性

**场景**: RPC调用过程中网络超时

**应对方案:**

```java
// 使用幂等性设计
@PostMapping("/social-connections/bind")
@Idempotent(key = "#request.memberId + #request.provider + #request.providerMemberId")
public R<SocialConnectionDTO> bindSocialAccount(@RequestBody BindSocialAccountRequest request) {
    // 实现逻辑
}

// 添加唯一索引防止重复绑定
ALTER TABLE member_social_connection 
ADD UNIQUE KEY uk_member_provider (member_id, provider, gmt_deleted_at);
```

### 风险3: 性能问题

**场景**: 频繁的RPC调用导致性能下降

**应对方案:**

```java
// 1. 批量查询接口
@GetMapping("/social-connections/batch")
R<List<SocialConnectionDTO>> batchGetSocialConnections(
    @RequestParam("memberIds") List<Long> memberIds
);

// 2. 缓存策略
@Cacheable(value = "social-connections", key = "#memberId")
public List<SocialConnectionDTO> getSocialConnections(Long memberId) {
    // 实现逻辑
}

// 3. 异步处理非关键操作
@Async
public void updateLastUsedTime(Long memberId, String provider) {
    // 更新最后使用时间
}
```

---

## 代码质量要求

### 1. 代码规范

- ✅ 遵循阿里巴巴Java开发手册
- ✅ 使用Lombok减少样板代码
- ✅ 完整的JavaDoc注释
- ✅ 统一的异常处理

### 2. 测试覆盖率

- ✅ 单元测试覆盖率 ≥ 80%
- ✅ 集成测试覆盖核心流程
- ✅ 异常场景测试

### 3. 日志规范

```java
// ✅ 正确的日志级别
log.info("[SocialConnectionsService] 创建社交连接, memberId={}, provider={}", 
    memberId, provider);

// ✅ 脱敏处理
log.info("[SocialConnectionsService] 绑定成功, openId={}", maskSensitiveInfo(openId));

// ✅ 异常日志
log.error("[SocialConnectionsService] 绑定失败, memberId={}, provider={}", 
    memberId, provider, exception);
```

### 4. 性能要求

- ✅ 接口响应时间 < 200ms (P99)
- ✅ RPC调用超时设置: 3s
- ✅ 数据库查询优化（建立索引）

---

## 总结

### 核心改进

1. **清晰的服务边界**: Auth服务负责认证流程，Member服务负责数据管理
2. **标准的跨服务调用**: 通过Facade接口进行服务间通信
3. **完整的业务封装**: SocialConnectionsService提供完整的业务逻辑
4. **高内聚低耦合**: 每个服务专注于自己的领域
5. **易于扩展**: 新增平台只需配置，无需修改核心代码

### 架构优势

- ✅ 符合微服务设计原则
- ✅ 符合领域驱动设计(DDD)
- ✅ 易于测试和维护
- ✅ 高可扩展性
- ✅ 良好的容错性

### 下一步计划

1. 按照Phase 1-3顺序实施
2. 每个Phase完成后进行Code Review
3. 完成后更新相关技术文档
4. 进行压力测试和性能调优

---

**文档版本控制:**
- v1.0 - 2025-11-04 - 初始版本
- 后续修改将记录在版本历史中

**审批流程:**
- [ ] 技术负责人审批
- [ ] 架构师审批
- [ ] 开始实施

---

**附录: 参考资料**

1. 《微服务设计模式》- Chris Richardson
2. 《领域驱动设计》- Eric Evans
3. 《阿里巴巴Java开发手册》
4. Spring Cloud OpenFeign官方文档
5. MyBatis Plus官方文档

