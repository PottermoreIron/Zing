package com.pot.auth.domain.wechat.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 微信用户信息实体
 *
 * <p>
 * 封装从微信开放平台获取的用户信息
 *
 * <p>
 * 字段说明：
 * <ul>
 * <li>openId: 用户唯一标识，在当前公众号/小程序下唯一</li>
 * <li>unionId: 用户在开放平台的唯一标识，可用于多应用账号关联</li>
 * <li>nickname: 用户昵称</li>
 * <li>avatar: 用户头像URL</li>
 * <li>country: 国家</li>
 * <li>province: 省份</li>
 * <li>city: 城市</li>
 * <li>sex: 性别（0-未知，1-男，2-女）</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-30
 */
@Getter
@Builder
public class WeChatUserInfo {

    /**
     * 微信用户唯一标识（OpenID）
     * <p>
     * 在当前公众号/小程序下唯一，不可跨应用使用
     */
    private final String openId;

    /**
     * 微信开放平台统一ID（UnionID）
     * <p>
     * 同一用户，在同一开放平台账号下的不同应用，unionId是相同的
     * <p>
     * 可选字段，需要开发者账号绑定开放平台
     */
    private final String unionId;

    /**
     * 用户昵称
     */
    private final String nickname;

    /**
     * 用户头像URL
     */
    private final String avatar;

    /**
     * 国家代码（如：CN）
     */
    private final String country;

    /**
     * 省份（如：Guangdong）
     */
    private final String province;

    /**
     * 城市（如：Shenzhen）
     */
    private final String city;

    /**
     * 性别
     * <ul>
     * <li>0 - 未知</li>
     * <li>1 - 男性</li>
     * <li>2 - 女性</li>
     * </ul>
     */
    private final Integer sex;

    /**
     * 访问令牌（可选）
     * <p>
     * 用于后续API调用，如获取用户详细信息、发送模板消息等
     */
    private final String accessToken;

    /**
     * 刷新令牌（可选）
     * <p>
     * 用于在访问令牌过期时获取新的访问令牌
     */
    private final String refreshToken;

    /**
     * 访问令牌过期时间（Unix时间戳，秒）
     */
    private final Long expiresAt;

    /**
     * 获取显示名称
     * <p>
     * 如果昵称为空，使用 openId 的一部分作为默认名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        // 使用 openId 后8位作为默认昵称
        if (openId != null && openId.length() > 8) {
            return "微信用户_" + openId.substring(openId.length() - 8);
        }
        return "微信用户";
    }

    /**
     * 获取用于创建用户的唯一标识
     * <p>
     * 优先使用 unionId（跨应用唯一），如果没有则使用 openId
     *
     * @return 唯一标识
     */
    public String getUniqueId() {
        return (unionId != null && !unionId.isBlank()) ? unionId : openId;
    }

    /**
     * 检查访问令牌是否已过期
     *
     * @return true-已过期，false-未过期
     */
    public boolean isAccessTokenExpired() {
        if (expiresAt == null) {
            return true;
        }
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

    @Override
    public String toString() {
        return "WeChatUserInfo{" +
                "openId='" + openId + '\'' +
                ", unionId='" + unionId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
