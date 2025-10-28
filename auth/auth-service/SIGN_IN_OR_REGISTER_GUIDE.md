# Sign In/Register 一键登录功能 - 使用文档

## 📋 功能概述

Sign In/Register 一键登录功能是一个智能化的用户认证系统，能够自动识别用户状态：

- **已注册用户**：直接登录
- **未注册用户**：自动注册后登录

## 🎯 核心优势

### 1. 用户体验优化

- ✅ **无需判断账号状态**：用户不需要先检查是否注册
- ✅ **一个接口搞定**：登录和注册合二为一
- ✅ **流程简化**：减少用户操作步骤

### 2. 架构设计亮点

- ✅ **策略模式**：支持多种认证方式，易于扩展
- ✅ **模板方法**：统一业务流程，代码复用率高
- ✅ **工厂模式**：自动路由，解耦合
- ✅ **零侵入**：完全复用现有 Login/Register 逻辑

### 3. 技术特性

- ✅ **类型安全**：泛型约束 + Jackson 多态反序列化
- ✅ **参数校验**：JSR-303 注解验证
- ✅ **异常处理**：统一异常捕获和响应
- ✅ **日志审计**：完整的操作日志记录

## 📦 项目结构

```
auth/auth-service/src/main/java/com/pot/auth/service/
├── enums/
│   └── SignInOrRegisterType.java          # 认证类型枚举
├── dto/
│   └── request/
│       └── signinorregister/
│           ├── SignInOrRegisterRequest.java           # 请求基类
│           ├── PhoneCodeSignInOrRegisterRequest.java  # 手机验证码请求
│           ├── EmailCodeSignInOrRegisterRequest.java  # 邮箱验证码请求
│           └── OAuth2SignInOrRegisterRequest.java     # OAuth2请求
├── strategy/
│   ├── SignInOrRegisterStrategy.java      # 策略接口
│   ├── impl/
│   │   └── signinorregister/
│   │       ├── AbstractSignInOrRegisterStrategy.java        # 抽象策略
│   │       ├── PhoneCodeSignInOrRegisterStrategy.java       # 手机策略实现
│   │       ├── EmailCodeSignInOrRegisterStrategy.java       # 邮箱策略实现
│   │       └── OAuth2SignInOrRegisterStrategy.java          # OAuth2策略实现
│   └── factory/
│       └── SignInOrRegisterStrategyFactory.java  # 策略工厂
├── service/
│   ├── SignInOrRegisterService.java       # 服务接口
│   └── impl/
│       └── SignInOrRegisterServiceImpl.java  # 服务实现
└── controller/
    └── SignInOrRegisterController.java    # REST控制器
```

## 🚀 API 使用指南

### 1. 手机验证码一键登录

#### 步骤1：发送验证码

```http
POST /auth/sign-in-or-register/verification-code/send
Content-Type: application/x-www-form-urlencoded

type=phone&target=13800138000
```

#### 步骤2：一键登录/注册

```http
POST /auth/sign-in-or-register
Content-Type: application/json

{
  "type": 1,
  "phone": "13800138000",
  "code": "123456"
}
```

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "userInfo": {
      "memberId": 100001,
      "nickname": "用户_abc123",
      "phone": "13800138000",
      "avatarUrl": "https://example.com/avatar/default.png",
      "status": 1
    }
  }
}
```

### 2. 邮箱验证码一键登录

#### 步骤1：发送验证码

```http
POST /auth/sign-in-or-register/verification-code/send
Content-Type: application/x-www-form-urlencoded

type=email&target=user@example.com
```

#### 步骤2：一键登录/注册

```http
POST /auth/sign-in-or-register
Content-Type: application/json

{
  "type": 2,
  "email": "user@example.com",
  "code": "123456"
}
```

### 3. 微信 OAuth2 一键登录

```http
POST /auth/sign-in-or-register
Content-Type: application/json

{
  "type": 3,
  "provider": "WECHAT",
  "code": "wx_auth_code_xxx",
  "redirectUri": "https://your-app.com/callback"
}
```

### 4. GitHub OAuth2 一键登录

```http
POST /auth/sign-in-or-register
Content-Type: application/json

{
  "type": 3,
  "provider": "GITHUB",
  "code": "github_auth_code_xxx",
  "redirectUri": "https://your-app.com/callback"
}
```

## 🔧 扩展指南

### 如何新增认证方式？

假设需要新增"人脸识别一键登录"，只需4步：

#### 1. 在枚举中添加类型

```java
// SignInOrRegisterType.java
FACE_RECOGNITION(4,"人脸识别一键登录");
```

#### 2. 创建请求DTO

```java
// FaceRecognitionSignInOrRegisterRequest.java
@Data
@EqualsAndHashCode(callSuper = true)
public class FaceRecognitionSignInOrRegisterRequest extends SignInOrRegisterRequest {
    public FaceRecognitionSignInOrRegisterRequest() {
        this.type = SignInOrRegisterType.FACE_RECOGNITION;
    }

    @NotBlank(message = "人脸特征数据不能为空")
    private String faceFeature;
}
```

#### 3. 在基类中注册子类型

```java
// SignInOrRegisterRequest.java
@JsonSubTypes({
        // ...existing types...
        @JsonSubTypes.Type(value = FaceRecognitionSignInOrRegisterRequest.class, name = "4")
})
```

#### 4. 实现策略类

```java

@Component
public class FaceRecognitionSignInOrRegisterStrategy
        extends AbstractSignInOrRegisterStrategy<FaceRecognitionSignInOrRegisterRequest> {

    @Override
    public SignInOrRegisterType getType() {
        return SignInOrRegisterType.FACE_RECOGNITION;
    }

    @Override
    protected void validateCredentials(FaceRecognitionSignInOrRegisterRequest request) {
        // 调用人脸识别服务验证
    }

    // ...实现其他抽象方法...
}
```

**完成！** Spring 会自动注入新策略，无需修改任何其他代码。

## 📊 业务流程图

```
┌─────────────────────────────────────────────────────────┐
│                    用户发起请求                          │
│              POST /auth/sign-in-or-register             │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              SignInOrRegisterController                  │
│                   (接收并校验参数)                        │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│             SignInOrRegisterService                      │
│              (通过工厂获取策略)                           │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│          SignInOrRegisterStrategyFactory                 │
│         (根据type路由到对应策略)                          │
└──────────────────────┬──────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    ┌────────┐   ┌────────┐   ┌────────┐
    │Phone   │   │Email   │   │OAuth2  │
    │Strategy│   │Strategy│   │Strategy│
    └───┬────┘   └───┬────┘   └───┬────┘
        │            │            │
        └────────────┴────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  1. 验证凭证           │
        │  2. 检查用户是否存在   │
        └────────────┬───────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
    ┌─────────┐           ┌──────────┐
    │已注册？ │           │未注册？  │
    └────┬────┘           └────┬─────┘
         │                     │
         ▼                     ▼
    ┌─────────┐           ┌──────────┐
    │直接登录 │           │注册+登录 │
    │(复用)   │           │(复用)    │
    └────┬────┘           └────┬─────┘
         │                     │
         └──────────┬──────────┘
                    │
                    ▼
        ┌────────────────────────┐
        │  返回 Token + 用户信息  │
        └────────────────────────┘
```

## 🔐 安全设计

### 1. 验证码安全

- ✅ **一次性使用**：验证成功后立即删除
- ✅ **有效期限制**：默认5分钟过期
- ✅ **频率限制**：同一目标60秒内只能发送一次
- ✅ **防暴力破解**：连续失败5次锁定10分钟

### 2. Token 安全

- ✅ **JWT签名**：防止篡改
- ✅ **有效期控制**：AccessToken 2小时，RefreshToken 7天
- ✅ **刷新机制**：支持无感刷新
- ✅ **黑名单机制**：支持强制下线

### 3. OAuth2 安全

- ✅ **HTTPS传输**：强制使用加密通道
- ✅ **State参数**：防止CSRF攻击
- ✅ **授权码模式**：最安全的OAuth2流程
- ✅ **Token绑定**：同一第三方账号只能绑定一个用户

## 📝 日志示例

### 新用户注册日志

```
2025-10-23 10:30:15 INFO  [SignInOrRegisterController] 收到一键登录/注册请求: type=PHONE_CODE
2025-10-23 10:30:15 INFO  [PhoneCodeSignInOrRegisterStrategy] 开始一键登录/注册流程: type=PHONE_CODE
2025-10-23 10:30:15 DEBUG [PhoneCodeSignInOrRegisterStrategy] 验证手机验证码: phone=138****8000
2025-10-23 10:30:15 DEBUG [PhoneCodeSignInOrRegisterStrategy] 手机验证码验证成功: phone=138****8000
2025-10-23 10:30:15 DEBUG [PhoneCodeSignInOrRegisterStrategy] 用户存在性检查: identifier=138****8000, exists=false
2025-10-23 10:30:15 INFO  [PhoneCodeSignInOrRegisterStrategy] 用户不存在，执行注册+登录流程: identifier=138****8000
2025-10-23 10:30:16 INFO  [PhoneCodeRegisterStrategy] 开始注册流程: type=PHONE_CODE
2025-10-23 10:30:16 INFO  [PhoneCodeRegisterStrategy] 手机验证码注册成功: phone=138****8000
2025-10-23 10:30:16 INFO  [PhoneCodeSignInOrRegisterStrategy] 新用户通过手机验证码注册成功: phone=138****8000, memberId=100001
2025-10-23 10:30:16 INFO  [SignInOrRegisterController] 一键登录/注册成功: type=PHONE_CODE, memberId=100001
```

### 老用户登录日志

```
2025-10-23 10:35:20 INFO  [SignInOrRegisterController] 收到一键登录/注册请求: type=PHONE_CODE
2025-10-23 10:35:20 INFO  [PhoneCodeSignInOrRegisterStrategy] 开始一键登录/注册流程: type=PHONE_CODE
2025-10-23 10:35:20 DEBUG [PhoneCodeSignInOrRegisterStrategy] 验证手机验证码: phone=138****8000
2025-10-23 10:35:20 DEBUG [PhoneCodeSignInOrRegisterStrategy] 手机验证码验证成功: phone=138****8000
2025-10-23 10:35:20 DEBUG [PhoneCodeSignInOrRegisterStrategy] 用户存在性检查: identifier=138****8000, exists=true
2025-10-23 10:35:20 INFO  [PhoneCodeSignInOrRegisterStrategy] 用户已存在，执行登录流程: identifier=138****8000
2025-10-23 10:35:20 INFO  [PhoneCodeLoginStrategy] 开始登录流程: type=PHONE_CODE
2025-10-23 10:35:20 INFO  [PhoneCodeLoginStrategy] 登录流程完成: type=PHONE_CODE, memberId=100001
2025-10-23 10:35:20 INFO  [PhoneCodeSignInOrRegisterStrategy] 用户通过手机验证码登录成功: phone=138****8000, memberId=100001
2025-10-23 10:35:20 INFO  [SignInOrRegisterController] 一键登录/注册成功: type=PHONE_CODE, memberId=100001
```

## 🎨 前端集成示例

### React 示例

```javascript
import axios from 'axios';

// 1. 发送验证码
const sendVerificationCode = async (phone) => {
    const response = await axios.post('/auth/sign-in-or-register/verification-code/send', null, {
        params: {type: 'phone', target: phone}
    });
    return response.data;
};

// 2. 一键登录
const signInOrRegister = async (phone, code) => {
    const response = await axios.post('/auth/sign-in-or-register', {
        type: 1,
        phone: phone,
        code: code
    });

    if (response.data.code === 200) {
        const {accessToken, refreshToken, userInfo} = response.data.data;

        // 保存Token
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);

        // 保存用户信息
        localStorage.setItem('userInfo', JSON.stringify(userInfo));

        return userInfo;
    } else {
        throw new Error(response.data.msg);
    }
};
```

### Vue 示例

```javascript
import {ref} from 'vue';
import axios from 'axios';

export default {
    setup() {
        const phone = ref('');
        const code = ref('');
        const loading = ref(false);

        const handleSignIn = async () => {
            loading.value = true;
            try {
                const response = await axios.post('/auth/sign-in-or-register', {
                    type: 1,
                    phone: phone.value,
                    code: code.value
                });

                const {accessToken, userInfo} = response.data.data;

                // 保存认证信息
                localStorage.setItem('token', accessToken);

                // 跳转到首页
                router.push('/home');
            } catch (error) {
                console.error('登录失败:', error);
            } finally {
                loading.value = false;
            }
        };

        return {phone, code, loading, handleSignIn};
    }
};
```

## 💡 最佳实践

### 1. 错误处理

```java
try{
AuthResponse response = signInOrRegisterService.signInOrRegister(request);
    return R.

success(response);
}catch(
BusinessException e){
        log.

error("一键登录失败: {}",e.getMessage());
        return R.

fail(e.getCode(),e.

getMessage());
        }catch(
Exception e){
        log.

error("系统异常",e);
    return R.

fail("系统繁忙，请稍后重试");
}
```

### 2. 异步处理

对于新用户注册，可以异步发送欢迎消息：

```java

@Override
protected void postProcess(PhoneCodeSignInOrRegisterRequest request,
                           AuthResponse response,
                           boolean isNewUser) {
    if (isNewUser) {
        // 异步发送欢迎消息
        CompletableFuture.runAsync(() -> {
            welcomeMessageService.send(response.getUserInfo().getMemberId());
        });

        // 异步发送新人礼包
        CompletableFuture.runAsync(() -> {
            giftService.grantNewUserGift(response.getUserInfo().getMemberId());
        });
    }
}
```

### 3. 监控告警

```java
// 监控登录成功率
if(isNewUser){
        metricsService.

increment("signin_or_register.new_user");
}else{
        metricsService.

increment("signin_or_register.existing_user");
}

// 监控登录耗时
long startTime = System.currentTimeMillis();
AuthResponse response = strategy.signInOrRegister(request);
long duration = System.currentTimeMillis() - startTime;
metricsService.

recordTime("signin_or_register.duration",duration);
```

## 📞 技术支持

如有问题或建议，请联系：

- 📧 Email: support@example.com
- 💬 Issues: https://github.com/yourproject/issues

---

**Version:** 1.0.0  
**Last Updated:** 2025-10-23  
**Author:** Pot

