package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import lombok.Builder;

/**
 * 创建用户命令（领域层）
 *
 * <p>
 * 用于从认证领域向用户模块传递创建用户的指令
 *
 * <p>
 * 支持的创建方式：
 * <ul>
 * <li>普通注册：username + password + email/phone</li>
 * <li>OAuth2注册：oauth2Provider + oauth2OpenId + email</li>
 * <li>微信注册：weChatOpenId + weChatUnionId + nickname</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Builder
public record CreateUserCommand(
        // ========== 基础信息 ==========
        String username,
        Email email,
        Phone phone,
        Password password,
        String avatarUrl,
        String firstName,
        String lastName,
        String nickname,
        boolean emailVerified,

        // ========== OAuth2 绑定信息 ==========
        /**
         * OAuth2提供商（如：google、github、facebook）
         */
        String oauth2Provider,

        /**
         * OAuth2用户唯一标识（OpenID）
         */
        String oauth2OpenId,

        // ========== 微信绑定信息 ==========
        /**
         * 微信 OpenID（在当前公众号/小程序下唯一）
         */
        String weChatOpenId,

        /**
         * 微信 UnionID（开放平台统一ID，可选）
         */
        String weChatUnionId) {
}
