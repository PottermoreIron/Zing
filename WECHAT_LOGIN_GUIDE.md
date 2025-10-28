# 微信扫码登录集成指南

## 📋 概述

本文档介绍如何集成和使用基于WxJava的微信扫码登录功能。该实现遵循工业级标准，具有高度的可扩展性和优雅的代码设计。

## 🏗️ 架构特点

### 设计模式应用

1. **策略模式** - `WeChatOAuth2LoginStrategy` 继承 `AbstractOAuth2LoginStrategy`
2. **工厂模式** - 通过 `OAuth2ClientFactory` 统一管理OAuth2客户端
3. **模板方法模式** - `WeChatOAuth2ClientService` 继承 `AbstractOAuth2ClientService`
4. **门面模式** - `WeChatQrCodeService` 封装扫码登录的所有业务逻辑

### 核心模块

```
auth-service/
├── oauth2/wechat/
│   ├── WeChatQrCodeService.java          # 扫码服务（核心业务逻辑）
│   ├── WeChatOAuth2ClientService.java    # 微信OAuth2客户端
│   └── config/WxJavaConfig.java          # 微信配置
├── strategy/impl/login/
│   └── WeChatOAuth2LoginStrategy.java    # 微信登录策略
├── controller/
│   └── WeChatOAuth2Controller.java       # 微信登录API
└── dto/wechat/
    ├── WeChatQrCodeResponse.java         # 二维码响应
    └── WeChatScanStatusResponse.java     # 扫码状态响应
```

## 🚀 快速开始

### 1. 申请微信开放平台账号

1. 访问 [微信开放平台](https://open.weixin.qq.com/)
2. 注册并完成开发者认证
3. 创建网站应用
4. 填写应用信息：
    - 应用名称
    - 应用简介
    - 应用官网
    - 授权回调域：`yourdomain.com`（不含http://）
5. 提交审核，审核通过后获得：
    - **AppID**（应用唯一标识）
    - **AppSecret**（应用密钥）

**文档**: https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html

### 2. 配置环境变量

```bash
# 启用微信登录
export WECHAT_ENABLED=true

# 微信开放平台 AppID
export WECHAT_OPEN_APPID="wx1234567890abcdef"

# 微信开放平台 AppSecret
export WECHAT_OPEN_APPSECRET="1234567890abcdef1234567890abcdef"

# 回调地址基础URL
export OAUTH2_REDIRECT_BASE_URL="https://yourdomain.com"
```

### 3. 配置文件已就绪

`application.yml` 已包含微信配置：

```yaml
oauth2:
  clients:
    wechat:
      enabled: ${WECHAT_ENABLED:false}
      client-id: ${WECHAT_OPEN_APPID:your_wechat_open_appid}
      client-secret: ${WECHAT_OPEN_APPSECRET:your_wechat_open_appsecret}
      authorization-uri: https://open.weixin.qq.com/connect/qrconnect
      token-uri: https://api.weixin.qq.com/sns/oauth2/access_token
      user-info-uri: https://api.weixin.qq.com/sns/userinfo
      redirect-uri: ${OAUTH2_REDIRECT_BASE_URL}/auth/wechat/callback
      scope: snsapi_login
```

## 📡 API接口说明

### 1. 获取微信扫码登录二维码

**接口**: `GET /auth/wechat/qrcode`

**说明**: 生成微信扫码登录二维码URL

**响应示例**:

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "qrCodeUrl": "https://open.weixin.qq.com/connect/qrconnect?appid=wx123&redirect_uri=https%3A%2F%2Fyourdomain.com%2Fauth%2Fwechat%2Fcallback&response_type=code&scope=snsapi_login&state=random_state_123#wechat_redirect",
    "state": "random_state_123",
    "expireSeconds": 300,
    "qrCodeId": "qr_abc123"
  }
}
```

### 2. 轮询扫码状态

**接口**: `GET /auth/wechat/scan-status?state={state}`

**说明**: 前端轮询此接口查询用户是否已扫码（建议1-2秒轮询一次）

**响应示例**:

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "status": "PENDING",  // PENDING-待扫码, SCANNED-已扫码, CONFIRMED-已确认, EXPIRED-已过期
    "message": "等待用户扫码"
  }
}
```

### 3. 微信授权回调

**接口**: `GET /auth/wechat/callback?code={code}&state={state}`

**说明**: 微信授权后回调此接口，完成登录

**响应示例**:

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "memberId": 123456,
    "username": "user123",
    "nickname": "微信用户",
    "avatarUrl": "https://thirdwx.qlogo.cn/..."
  }
}
```

### 4. 手动触发登录

**接口**: `POST /auth/wechat/login?code={code}&state={state}`

**说明**: 前端接收到code后，主动调用此接口完成登录

## 🖥️ 前端集成示例

### Vue 3 + TypeScript 示例

```typescript
// wechat-login.ts
import {ref, onUnmounted} from 'vue'
import QRCode from 'qrcode'

export function useWeChatLogin() {
    const qrCodeUrl = ref('')
    const scanStatus = ref('PENDING')
    const loading = ref(false)
    let pollTimer: number | null = null

    // 1. 获取二维码
    const getQrCode = async () => {
        loading.value = true
        try {
            const res = await fetch('/auth/wechat/qrcode')
            const data = await res.json()

            if (data.code === 200) {
                // 生成二维码图片
                const canvas = document.getElementById('qrcode-canvas')
                await QRCode.toCanvas(canvas, data.data.qrCodeUrl, {
                    width: 300,
                    margin: 2
                })

                // 开始轮询扫码状态
                startPolling(data.data.state)
            }
        } catch (error) {
            console.error('获取二维码失败:', error)
        } finally {
            loading.value = false
        }
    }

    // 2. 轮询扫码状态
    const startPolling = (state: string) => {
        pollTimer = setInterval(async () => {
            try {
                const res = await fetch(`/auth/wechat/scan-status?state=${state}`)
                const data = await res.json()

                if (data.code === 200) {
                    scanStatus.value = data.data.status

                    // 如果已确认，停止轮询并处理登录
                    if (data.data.status === 'CONFIRMED') {
                        stopPolling()
                        handleLoginSuccess(data.data.code)
                    }

                    // 如果已过期，停止轮询
                    if (data.data.status === 'EXPIRED') {
                        stopPolling()
                    }
                }
            } catch (error) {
                console.error('轮询状态失败:', error)
            }
        }, 2000) // 每2秒轮询一次
    }

    // 3. 停止轮询
    const stopPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer)
            pollTimer = null
        }
    }

    // 4. 处理登录成功
    const handleLoginSuccess = async (code: string) => {
        // 这里code已经通过callback接口处理，可以直接获取token
        // 或者前端主动调用login接口
        console.log('登录成功')
    }

    // 清理
    onUnmounted(() => {
        stopPolling()
    })

    return {
        qrCodeUrl,
        scanStatus,
        loading,
        getQrCode,
        stopPolling
    }
}
```

### React 示例

```tsx
// WeChatLogin.tsx
import React, {useState, useEffect, useRef} from 'react'
import QRCode from 'qrcode'

export const WeChatLogin: React.FC = () => {
    const [scanStatus, setScanStatus] = useState('PENDING')
    const [loading, setLoading] = useState(false)
    const canvasRef = useRef<HTMLCanvasElement>(null)
    const pollTimerRef = useRef<number | null>(null)

    // 获取二维码
    const getQrCode = async () => {
        setLoading(true)
        try {
            const res = await fetch('/auth/wechat/qrcode')
            const data = await res.json()

            if (data.code === 200 && canvasRef.current) {
                await QRCode.toCanvas(canvasRef.current, data.data.qrCodeUrl, {
                    width: 300,
                    margin: 2
                })
                startPolling(data.data.state)
            }
        } catch (error) {
            console.error('获取二维码失败:', error)
        } finally {
            setLoading(false)
        }
    }

    // 轮询扫码状态
    const startPolling = (state: string) => {
        pollTimerRef.current = window.setInterval(async () => {
            try {
                const res = await fetch(`/auth/wechat/scan-status?state=${state}`)
                const data = await res.json()

                if (data.code === 200) {
                    setScanStatus(data.data.status)

                    if (data.data.status === 'CONFIRMED' || data.data.status === 'EXPIRED') {
                        stopPolling()
                    }
                }
            } catch (error) {
                console.error('轮询失败:', error)
            }
        }, 2000)
    }

    const stopPolling = () => {
        if (pollTimerRef.current) {
            clearInterval(pollTimerRef.current)
            pollTimerRef.current = null
        }
    }

    useEffect(() => {
        return () => stopPolling()
    }, [])

    return (
        <div className="wechat-login">
            <canvas ref={canvasRef} id="qrcode-canvas"/>
            <div className="status">
                {scanStatus === 'PENDING' && '请使用微信扫码'}
                {scanStatus === 'SCANNED' && '已扫码，请在手机上确认'}
                {scanStatus === 'CONFIRMED' && '登录成功！'}
                {scanStatus === 'EXPIRED' && '二维码已过期，请刷新'}
            </div>
            <button onClick={getQrCode} disabled={loading}>
                {loading ? '生成中...' : '获取二维码'}
            </button>
        </div>
    )
}
```

## 🔐 安全特性

1. **State参数防CSRF攻击** - 每次生成二维码时创建唯一state，验证回调时验证state有效性
2. **Redis缓存管理** - State和扫码状态存储在Redis中，自动过期（5分钟）
3. **接口限流** - 使用 `@RateLimit` 注解防止接口滥用
4. **UnionID账号统一** - 支持同一微信账号在多个应用间账号关联

## 🔄 扩展功能

### 支持微信小程序登录

只需新增 `WeChatMiniProgramLoginStrategy`，复用现有OAuth2架构：

```java

@Service
public class WeChatMiniProgramLoginStrategy extends AbstractOAuth2LoginStrategy {
    // 实现小程序登录逻辑
}
```

### 支持微信公众号登录

新增 `WeChatMpLoginStrategy`，修改scope为 `snsapi_userinfo`

## 📊 流程图

```
用户端                    前端                    后端                    微信服务器
  |                      |                      |                       |
  |---点击微信登录------->|                      |                       |
  |                      |----获取二维码-------->|                       |
  |                      |<---返回二维码URL------|                       |
  |                      |                      |                       |
  |<--展示二维码----------|                      |                       |
  |                      |                      |                       |
  |                      |----轮询状态(每2秒)---->|                       |
  |                      |<---PENDING-----------|                       |
  |                      |                      |                       |
  |--使用微信扫码-------->|                      |                       |
  |                      |                      |                       |
  |                      |----轮询状态---------->|                       |
  |                      |<---SCANNED----------|                       |
  |                      |                      |                       |
  |--在手机上确认授权---->|                      |                       |
  |                      |                      |<--微信回调(code)------|
  |                      |                      |                       |
  |                      |                      |--获取access_token---->|
  |                      |                      |<--返回token----------|
  |                      |                      |                       |
  |                      |                      |--获取用户信息-------->|
  |                      |                      |<--返回用户信息--------|
  |                      |                      |                       |
  |                      |<---登录成功(JWT)-----|                       |
  |<--跳转到首页---------|                      |                       |
```

## 🐛 常见问题

### Q1: 二维码生成失败

**A**: 检查是否配置了正确的 `WECHAT_OPEN_APPID` 和 `WECHAT_OPEN_APPSECRET`

### Q2: 回调地址不匹配

**A**: 确保微信开放平台配置的授权回调域与 `OAUTH2_REDIRECT_BASE_URL` 一致

### Q3: 获取用户信息失败

**A**: 确保在微信开放平台申请了 `snsapi_login` 权限

### Q4: UnionID为空

**A**: 只有在微信开放平台绑定了多个应用时才会返回UnionID

## 📝 数据库表设计建议

为了支持微信登录，建议在Member表中添加以下字段：

```sql
ALTER TABLE member ADD COLUMN union_id VARCHAR(64) COMMENT '微信UnionID';
ALTER TABLE member ADD COLUMN wechat_open_id VARCHAR(64) COMMENT '微信OpenID';
ALTER TABLE member ADD COLUMN oauth2_bindings JSON COMMENT 'OAuth2绑定信息';

CREATE INDEX idx_union_id ON member(union_id);
CREATE INDEX idx_wechat_open_id ON member(wechat_open_id);
```

## 📚 参考文档

- [微信开放平台文档](https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html)
- [WxJava GitHub](https://github.com/binarywang/WxJava)
- [OAuth2 RFC 6749](https://tools.ietf.org/html/rfc6749)

## 🎉 总结

微信扫码登录功能已完整实现，具有以下特点：

✅ **架构专业** - 策略模式 + 工厂模式 + 模板方法模式  
✅ **工业级别** - 完善的错误处理、日志记录、安全防护  
✅ **代码优雅** - 清晰的模块划分、统一的命名规范  
✅ **高扩展性** - 易于支持小程序、公众号等其他微信登录方式  

