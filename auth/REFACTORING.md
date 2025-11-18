# 认证系统重构文档

## 重构概述

本次重构采用**策略模式 + 工厂模式 + Jackson多态序列化**的架构，实现了工业级的可扩展认证系统。

### 核心架构决策

1. **三层策略体系**
   - `LoginStrategy` - 传统登录策略（用户名/手机/邮箱 + 密码/验证码）
   - `RegisterStrategy` - 传统注册策略（用户名/手机/邮箱 + 密码/验证码）
   - `AuthenticationStrategy` - 一体化认证策略（OAuth2/WeChat）

2. **OAuth2/WeChat独立设计的原因**
   - 注册登录合二为一，无需用户显式选择
   - 请求参数完全不同（code/state vs username/password）
   - 业务流程不同（自动注册 vs 显式注册）
   - 符合单一职责原则

3. **Jackson多态序列化**
   - 使用`sealed interface`限制子类型，保证类型安全
   - 通过`@JsonTypeInfo`和`@JsonSubTypes`自动识别请求类型
   - 所有请求DTO使用`record`实现，简洁且不可变

## 目录结构

```
auth-service/
├── domain/
│   └── strategy/
│       ├── LoginStrategy.java                    # 登录策略接口
│       ├── RegisterStrategy.java                 # 注册策略接口
│       ├── AuthenticationStrategy.java           # 一体化认证策略接口
│       ├── AbstractLoginStrategy.java            # 登录策略抽象模板类
│       ├── AbstractRegisterStrategy.java         # 注册策略抽象模板类
│       ├── factory/
│       │   ├── LoginStrategyFactory.java         # 登录策略工厂
│       │   ├── RegisterStrategyFactory.java      # 注册策略工厂
│       │   └── AuthenticationStrategyFactory.java # 认证策略工厂
│       ├── login/                                # 5个登录策略实现
│       │   ├── UsernamePasswordLoginStrategy.java
│       │   ├── EmailPasswordLoginStrategy.java
│       │   ├── PhonePasswordLoginStrategy.java
│       │   ├── EmailCodeLoginStrategy.java
│       │   └── PhoneCodeLoginStrategy.java
│       ├── register/                             # 5个注册策略实现
│       │   ├── UsernamePasswordRegisterStrategy.java
│       │   ├── EmailPasswordRegisterStrategy.java
│       │   ├── PhonePasswordRegisterStrategy.java
│       │   ├── EmailCodeRegisterStrategy.java
│       │   └── PhoneCodeRegisterStrategy.java
│       └── authentication/                       # 2个一体化认证策略
│           ├── OAuth2AuthenticationStrategy.java
│           └── WeChatAuthenticationStrategy.java
├── application/
│   └── service/
│       ├── LoginApplicationService.java          # 登录应用服务（重构）
│       └── RegistrationApplicationService.java   # 注册应用服务（重构）
└── interfaces/
    ├── dto/auth/
    │   ├── LoginRequest.java                     # 登录请求基接口（sealed）
    │   ├── RegisterRequest.java                  # 注册请求基接口（sealed）
    │   ├── UsernamePasswordLoginRequest.java     # 用户名密码登录请求（record）
    │   ├── EmailPasswordLoginRequest.java
    │   ├── PhonePasswordLoginRequest.java
    │   ├── EmailCodeLoginRequest.java
    │   ├── PhoneCodeLoginRequest.java
    │   ├── OAuth2LoginRequest.java
    │   ├── WeChatLoginRequest.java
    │   ├── UsernamePasswordRegisterRequest.java  # 用户名密码注册请求（record）
    │   ├── EmailPasswordRegisterRequest.java
    │   ├── PhonePasswordRegisterRequest.java
    │   ├── EmailCodeRegisterRequest.java
    │   ├── PhoneCodeRegisterRequest.java
    │   ├── OAuth2RegisterRequest.java
    │   └── WeChatRegisterRequest.java
    └── controller/
        ├── AuthenticationControllerV2.java        # 统一登录端点
        └── RegistrationControllerV2.java          # 统一注册端点
```

## 支持的认证方式

### 登录方式（7种）

| 类型 | loginType | 请求DTO | 策略类 |
|------|-----------|---------|--------|
| 用户名密码登录 | USERNAME_PASSWORD | UsernamePasswordLoginRequest | UsernamePasswordLoginStrategy |
| 邮箱密码登录 | EMAIL_PASSWORD | EmailPasswordLoginRequest | EmailPasswordLoginStrategy |
| 手机号密码登录 | PHONE_PASSWORD | PhonePasswordLoginRequest | PhonePasswordLoginStrategy |
| 邮箱验证码登录 | EMAIL_CODE | EmailCodeLoginRequest | EmailCodeLoginStrategy |
| 手机号验证码登录 | PHONE_CODE | PhoneCodeLoginRequest | PhoneCodeLoginStrategy |
| OAuth2登录 | OAUTH2 | OAuth2LoginRequest | OAuth2AuthenticationStrategy |
| 微信登录 | WECHAT | WeChatLoginRequest | WeChatAuthenticationStrategy |

### 注册方式（7种）

| 类型 | registerType | 请求DTO | 策略类 |
|------|--------------|---------|--------|
| 用户名密码注册 | USERNAME_PASSWORD | UsernamePasswordRegisterRequest | UsernamePasswordRegisterStrategy |
| 邮箱密码注册 | EMAIL_PASSWORD | EmailPasswordRegisterRequest | EmailPasswordRegisterStrategy |
| 手机号密码注册 | PHONE_PASSWORD | PhonePasswordRegisterRequest | PhonePasswordRegisterStrategy |
| 邮箱验证码注册 | EMAIL_CODE | EmailCodeRegisterRequest | EmailCodeRegisterStrategy |
| 手机号验证码注册 | PHONE_CODE | PhoneCodeRegisterRequest | PhoneCodeRegisterStrategy |
| OAuth2注册 | OAUTH2 | OAuth2RegisterRequest | OAuth2AuthenticationStrategy |
| 微信注册 | WECHAT | WeChatRegisterRequest | WeChatAuthenticationStrategy |

## API使用示例

### 统一登录端点

**POST /auth/v2/login**

#### 1. 用户名密码登录

```json
{
  "loginType": "USERNAME_PASSWORD",
  "username": "john_doe",
  "password": "Password123!",
  "userDomain": "MEMBER"
}
```

#### 2. 邮箱验证码登录

```json
{
  "loginType": "EMAIL_CODE",
  "email": "john@example.com",
  "verificationCode": "123456",
  "userDomain": "MEMBER"
}
```

#### 3. OAuth2登录（Google）

```json
{
  "loginType": "OAUTH2",
  "provider": "google",
  "code": "4/0AY0e-g7xxxxxxxxxxx",
  "state": "random_state",
  "userDomain": "MEMBER"
}
```

#### 4. 微信登录

```json
{
  "loginType": "WECHAT",
  "code": "041xxxxxx",
  "state": "random_state",
  "userDomain": "MEMBER"
}
```

### 统一注册端点

**POST /auth/v2/register**

#### 1. 用户名密码注册

```json
{
  "registerType": "USERNAME_PASSWORD",
  "username": "john_doe",
  "password": "Password123!",
  "userDomain": "MEMBER"
}
```

#### 2. 手机号验证码注册

```json
{
  "registerType": "PHONE_CODE",
  "phone": "13800138000",
  "verificationCode": "123456",
  "userDomain": "MEMBER"
}
```

## 工作流程

### 登录流程

```
Client Request (JSON)
    ↓
Jackson 反序列化 (根据loginType字段)
    ↓
Controller 接收多态LoginRequest
    ↓
LoginApplicationService.login()
    ├─ 判断登录类型
    ├─ 传统登录 → LoginStrategyFactory.getStrategy()
    │   └─ 执行 LoginStrategy.execute()
    │       ├─ validateRequest()
    │       ├─ doLogin() (子类实现)
    │       └─ generateAuthenticationResult()
    └─ OAuth2/WeChat → AuthenticationStrategyFactory.getStrategy()
        └─ 执行 AuthenticationStrategy.authenticate()
            ├─ 获取第三方用户信息
            ├─ 查找或创建用户
            └─ 生成Token
    ↓
返回 AuthenticationResult
    ↓
转换为 LoginResponse
    ↓
返回给客户端
```

### 注册流程

```
Client Request (JSON)
    ↓
Jackson 反序列化 (根据registerType字段)
    ↓
Controller 接收多态RegisterRequest
    ↓
RegistrationApplicationService.register()
    ├─ 判断注册类型
    ├─ 传统注册 → RegisterStrategyFactory.getStrategy()
    │   └─ 执行 RegisterStrategy.execute()
    │       ├─ validateRequest()
    │       ├─ doRegister() (子类实现)
    │       └─ generateAuthenticationResult()
    └─ OAuth2/WeChat → AuthenticationStrategyFactory.getStrategy()
        └─ 执行 AuthenticationStrategy.authenticate()
            └─ 自动处理注册或登录
    ↓
返回 AuthenticationResult
    ↓
转换为 RegisterResponse
    ↓
返回给客户端
```

## 扩展性设计

### 添加新的认证方式

假设要添加"指纹登录"：

#### 1. 定义请求DTO

```java
public record FingerprintLoginRequest(
    @NotBlank String loginType,
    @NotBlank String fingerprintData,
    String userDomain
) implements LoginRequest {}
```

#### 2. 更新LoginRequest sealed interface

```java
@JsonSubTypes({
    // ...existing types...
    @JsonSubTypes.Type(value = FingerprintLoginRequest.class, name = "FINGERPRINT")
})
public sealed interface LoginRequest permits
    UsernamePasswordLoginRequest,
    // ...existing types...
    FingerprintLoginRequest {
    // ...
}
```

#### 3. 实现策略类

```java
@Component
public class FingerprintLoginStrategy extends AbstractLoginStrategy {
    
    @Override
    protected void validateRequest(LoginRequest request) {
        // 验证逻辑
    }
    
    @Override
    protected UserDTO doLogin(LoginRequest request, LoginContext loginContext) {
        // 指纹登录核心逻辑
    }
    
    @Override
    public boolean supports(String loginType) {
        return "FINGERPRINT".equals(loginType);
    }
}
```

#### 4. 完成！

- 无需修改 Controller
- 无需修改 Application Service
- 无需修改 Factory（自动注册）
- 只需添加策略实现和DTO

## 技术亮点

### 1. 策略模式 + 模板方法模式

- `AbstractLoginStrategy`和`AbstractRegisterStrategy`封装通用流程
- 子类只需实现`validateRequest()`和`doLogin()`/`doRegister()`
- 避免重复代码，统一异常处理和日志记录

### 2. 工厂模式自动注册

- Spring自动注入`List<LoginStrategy>`
- 工厂通过`supports()`方法动态匹配策略
- 新增策略自动注册，无需修改工厂代码

### 3. Jackson多态序列化

- 使用`@JsonTypeInfo`和`@JsonSubTypes`
- 根据`loginType`/`registerType`字段自动反序列化
- 类型安全，编译时检查

### 4. Sealed Interface（Java 17+）

- 限制`LoginRequest`和`RegisterRequest`的子类型
- 编译器保证所有可能的类型都已处理
- 增强类型安全性

### 5. Record 不可变DTO

- 简洁的DTO定义
- 天然不可变，线程安全
- 自动生成equals/hashCode/toString

## 注意事项

### 1. 旧版API兼容

- 保留了`AuthenticationController`和`RegistrationController`
- 新版API使用`V2`后缀：`AuthenticationControllerV2`和`RegistrationControllerV2`
- 端点：`/auth/v2/login`和`/auth/v2/register`
- 可以逐步迁移客户端到新API

### 2. 错误处理

- 未找到策略时抛出`DomainException(UNSUPPORTED_*_TYPE)`
- 所有策略继承的异常处理逻辑统一
- 日志记录完整

### 3. 性能优化

- 策略Map使用`ConcurrentHashMap`
- 策略实例在Spring容器中单例
- 无反射调用，性能优秀

## 测试建议

### 1. 单元测试

```java
@Test
void testUsernamePasswordLogin() {
    LoginRequest request = new UsernamePasswordLoginRequest(
        "USERNAME_PASSWORD",
        "john_doe",
        "Password123!",
        "MEMBER"
    );
    
    LoginResponse response = loginApplicationService.login(
        request,
        "127.0.0.1",
        "Mozilla/5.0"
    );
    
    assertNotNull(response.accessToken());
}
```

### 2. 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerV2Test {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testLoginWithUsername() throws Exception {
        mockMvc.perform(post("/auth/v2/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "loginType": "USERNAME_PASSWORD",
                    "username": "john_doe",
                    "password": "Password123!",
                    "userDomain": "MEMBER"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }
}
```

## 总结

本次重构实现了：

✅ 7种登录方式和7种注册方式的统一管理  
✅ 工业级可扩展架构  
✅ 代码优雅，职责清晰  
✅ 类型安全，编译时检查  
✅ 易于测试和维护  
✅ OAuth2/WeChat一体化认证的合理设计  
✅ 遵循SOLID原则  

符合企业级应用开发标准，可以直接应用于生产环境。

