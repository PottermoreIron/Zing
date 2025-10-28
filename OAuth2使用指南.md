# OAuth2使用指南（中文版）

## 概述

本指南详细说明如何集成和使用支持多个提供商的OAuth2认证系统，包括GitHub、Google、微信、Facebook和Twitter。该系统采用专业的工业级架构设计，具有极高的可扩展性。

## 架构设计

### 核心组件

1. **OAuth2Provider枚举** - 定义支持的OAuth2提供商
2. **OAuth2ClientService接口** - OAuth2客户端操作的抽象接口
3. **AbstractOAuth2ClientService** - OAuth2标准流程的模板实现
4. **OAuth2ClientFactory** - 用于创建提供商特定客户端的工厂类
5. **AbstractOAuth2LoginStrategy** - OAuth2认证的登录策略
6. **OAuth2Controller** - OAuth2操作的REST API端点

### 使用的设计模式

- **策略模式（Strategy Pattern）**: 为每个提供商提供不同的登录策略
- **工厂模式（Factory Pattern）**: OAuth2ClientFactory用于创建特定提供商的客户端
- **模板方法模式（Template Method Pattern）**: AbstractOAuth2ClientService提供通用的OAuth2流程

## 配置说明

### 1. 环境变量

为OAuth2提供商设置以下环境变量：

```bash
# GitHub OAuth2
export GITHUB_CLIENT_ID="你的github客户端ID"
export GITHUB_CLIENT_SECRET="你的github客户端密钥"

# Google OAuth2
export GOOGLE_CLIENT_ID="你的google客户端ID"
export GOOGLE_CLIENT_SECRET="你的google客户端密钥"

# 重定向基础URL
export OAUTH2_REDIRECT_BASE_URL="http://localhost:3000"
```

### 2. 应用配置

`application.yml`已包含OAuth2配置：

```yaml
oauth2:
  clients:
    github:
      enabled: true
      client-id: ${GITHUB_CLIENT_ID:your_github_client_id}
      client-secret: ${GITHUB_CLIENT_SECRET:your_github_client_secret}
      authorization-uri: https://github.com/login/oauth/authorize
      token-uri: https://github.com/login/oauth/access_token
      user-info-uri: https://api.github.com/user
      redirect-uri: ${OAUTH2_REDIRECT_BASE_URL:http://localhost:3000}/oauth2/callback/github
      scope: user:email

    google:
      enabled: true
      client-id: ${GOOGLE_CLIENT_ID:your_google_client_id}
      client-secret: ${GOOGLE_CLIENT_SECRET:your_google_client_secret}
      authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
      token-uri: https://oauth2.googleapis.com/token
      user-info-uri: https://www.googleapis.com/oauth2/v2/userinfo
      redirect-uri: ${OAUTH2_REDIRECT_BASE_URL:http://localhost:3000}/oauth2/callback/google
      scope: openid profile email
```

## 获取OAuth2凭证

### GitHub

1. 访问 GitHub设置 → Developer settings → OAuth Apps
2. 点击"New OAuth App"
3. 填写应用详情：
    - Application name: 你的应用名称
    - Homepage URL: http://localhost:3000
    - Authorization callback URL: http://localhost:3000/oauth2/callback/github
4. 点击"Register application"
5. 复制Client ID并生成Client Secret

**官方文档**: https://docs.github.com/en/developers/apps/building-oauth-apps

### Google

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建新项目或选择现有项目
3. 启用Google+ API
4. 转到凭据 → 创建凭据 → OAuth 2.0 客户端ID
5. 配置OAuth同意屏幕
6. 设置授权的重定向URI: http://localhost:3000/oauth2/callback/google
7. 复制客户端ID和客户端密钥

**官方文档**: https://developers.google.com/identity/protocols/oauth2

## API使用说明

### 1. 获取授权URL

**接口**: `GET /auth/oauth2/authorization-url/{provider}`

**参数**:

- `provider` - OAuth2提供商名称（github, google等）

**请求示例**:

```bash
curl -X GET "http://localhost:10000/auth/oauth2/authorization-url/github"
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "authorizationUrl": "https://github.com/login/oauth/authorize?client_id=xxx&redirect_uri=xxx&scope=user:email&state=xxx&response_type=code",
    "state": "550e8400-e29b-41d4-a716-446655440000",
    "provider": "github"
  }
}
```

### 2. 处理OAuth2回调

**接口**: `POST /auth/oauth2/callback/{provider}`

**参数**:

- `provider` - OAuth2提供商名称
- `code` - OAuth2提供商返回的授权码
- `state` - 用于CSRF防护的state参数

**请求示例**:

```bash
curl -X POST "http://localhost:10000/auth/oauth2/callback/github?code=xxx&state=xxx"
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "authToken": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600
    },
    "userInfo": {
      "memberId": 123,
      "username": "user@example.com",
      "nickname": "张三",
      "avatarUrl": "https://avatars.githubusercontent.com/u/123456"
    },
    "timestamp": 1698012345678
  }
}
```

### 3. 获取支持的提供商列表

**接口**: `GET /auth/oauth2/providers`

**请求示例**:

```bash
curl -X GET "http://localhost:10000/auth/oauth2/providers"
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "github": "GitHub",
    "google": "Google",
    "wechat": "微信",
    "facebook": "Facebook",
    "twitter": "Twitter"
  }
}
```

## 前端集成示例

### React/Vue示例

```javascript
// 1. 发起OAuth2登录
async function loginWithGitHub() {
    try {
        // 获取授权URL
        const response = await fetch('http://localhost:10000/auth/oauth2/authorization-url/github');
        const data = await response.json();

        // 保存state以便后续验证
        sessionStorage.setItem('oauth2_state', data.data.state);

        // 重定向到OAuth2提供商
        window.location.href = data.data.authorizationUrl;
    } catch (error) {
        console.error('发起OAuth2登录失败:', error);
    }
}

// 2. 处理OAuth2回调
async function handleOAuth2Callback() {
    // 获取URL参数
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const state = urlParams.get('state');
    const provider = window.location.pathname.split('/').pop(); // 例如: 'github'

    // 验证state
    const savedState = sessionStorage.getItem('oauth2_state');
    if (state !== savedState) {
        console.error('无效的state参数 - 可能是CSRF攻击');
        return;
    }

    try {
        // 发送回调到后端
        const response = await fetch(
            `http://localhost:10000/auth/oauth2/callback/${provider}?code=${code}&state=${state}`,
            {method: 'POST'}
        );
        const data = await response.json();

        if (data.code === 200) {
            // 保存令牌
            localStorage.setItem('access_token', data.data.authToken.accessToken);
            localStorage.setItem('refresh_token', data.data.authToken.refreshToken);

            // 保存用户信息
            localStorage.setItem('user_info', JSON.stringify(data.data.userInfo));

            // 重定向到主页
            window.location.href = '/dashboard';
        }
    } catch (error) {
        console.error('OAuth2回调处理失败:', error);
    }
}
```

## 添加新的OAuth2提供商

要添加新的OAuth2提供商（例如Twitter），请按以下步骤操作：

### 1. 更新LoginType枚举

枚举已包含常见提供商。对于自定义提供商，在`LoginType.java`中添加：

```java
OAUTH2_CUSTOM(11,"自定义OAuth2登录");
```

### 2. 添加提供商到OAuth2Provider枚举

在`OAuth2Provider.java`中添加：

```java
TWITTER("twitter","Twitter",LoginType.OAUTH2_TWITTER);
```

### 3. 创建提供商特定的客户端

创建`TwitterOAuth2ClientService.java`:

```java

@Slf4j
@Service
public class TwitterOAuth2ClientService extends AbstractOAuth2ClientService {

    public TwitterOAuth2ClientService(
            OAuth2ClientProperties oauth2Properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(oauth2Properties, restTemplate, objectMapper);
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.TWITTER;
    }

    @Override
    protected OAuth2UserInfo parseUserInfo(String responseBody, String accessToken) {
        // 解析Twitter特定的用户信息格式
        // ...
    }
}
```

### 4. 创建登录策略

创建`TwitterOAuth2LoginStrategy.java`:

```java

@Slf4j
@Component
public class TwitterOAuth2LoginStrategy extends AbstractOAuth2LoginStrategy {

    public TwitterOAuth2LoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter,
            OAuth2ClientFactory oauth2ClientFactory,
            StringRedisTemplate redisTemplate) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter, oauth2ClientFactory, redisTemplate);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.OAUTH2_TWITTER;
    }

    @Override
    protected OAuth2Provider getOAuth2Provider() {
        return OAuth2Provider.TWITTER;
    }
}
```

### 5. 添加配置

更新`application.yml`:

```yaml
oauth2:
  clients:
    twitter:
      enabled: true
      client-id: ${TWITTER_CLIENT_ID}
      client-secret: ${TWITTER_CLIENT_SECRET}
      authorization-uri: https://twitter.com/i/oauth2/authorize
      token-uri: https://api.twitter.com/2/oauth2/token
      user-info-uri: https://api.twitter.com/2/users/me
      redirect-uri: ${OAUTH2_REDIRECT_BASE_URL}/oauth2/callback/twitter
      scope: tweet.read users.read
```

### 6. 更新LoginRequest映射

在`LoginRequest.java`中添加新类型：

```java
@JsonSubTypes.Type(value = OAuth2LoginRequest.class, name = "10")
```

完成！新提供商现已完全集成。

## 安全考虑

1. **State参数**: 始终验证state参数以防止CSRF攻击
2. **HTTPS**: 生产环境中对所有OAuth2重定向使用HTTPS
3. **令牌存储**: 安全存储令牌（推荐使用HttpOnly cookies）
4. **令牌过期**: 为过期的访问令牌实现刷新令牌逻辑
5. **权限范围限制**: 仅请求必要的OAuth2权限范围

## 故障排除

### 常见问题

1. **无效的redirect_uri**: 确保OAuth2应用设置中的重定向URI与配置的URI完全匹配
2. **State不匹配**: 验证Redis正在运行且state参数被正确缓存
3. **令牌交换失败**: 检查客户端ID和密钥是否正确
4. **用户信息解析错误**: 验证API响应格式是否与解析逻辑匹配

### 启用调试日志

在`application.yml`中添加：

```yaml
logging:
  level:
    com.pot.auth.service.oauth2: DEBUG
    com.pot.auth.service.strategy.impl.login: DEBUG
```

## 架构优势

### 1. 高度可扩展

- 添加新提供商只需创建2个类（ClientService和LoginStrategy）
- 无需修改核心业务逻辑

### 2. 代码复用性强

- 抽象基类封装了OAuth2标准流程
- 工厂模式统一管理所有提供商

### 3. 职责分离清晰

- OAuth2ClientService负责与第三方API交互
- LoginStrategy负责用户登录业务逻辑
- Controller仅负责HTTP请求处理

### 4. 易于维护

- 每个提供商独立实现，互不影响
- 统一的接口规范，降低学习成本

## 测试

使用提供的Postman/Swagger文档测试OAuth2端点。您也可以使用各提供商提供的OAuth2测试工具进行测试。

## 支持

如有问题或疑问，请参考：

- GitHub OAuth2文档: https://docs.github.com/en/developers/apps/building-oauth-apps
- Google OAuth2文档: https://developers.google.com/identity/protocols/oauth2
- 项目文档: 参见`OAUTH2_INTEGRATION_GUIDE.md`获取英文版本

