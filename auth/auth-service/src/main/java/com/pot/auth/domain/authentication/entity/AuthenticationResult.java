package com.pot.auth.domain.authentication.entity;

import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

/**
 * 认证结果值对象
 *
 * <p>封装认证成功后的完整信息
 * <p><strong>设计决策：使用record</strong>
 * <ul>
 *   <li>✅ 不可变 - 认证结果一旦创建就不应改变</li>
 *   <li>✅ 值语义 - 只关心内容，不关心身份</li>
 *   <li>✅ 无业务行为 - 纯数据载体</li>
 *   <li>✅ 自动生成equals/hashCode/toString</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
@Builder
public record AuthenticationResult(
        UserId userId,
        UserDomain userDomain,
        String username,
        String email,
        String phone,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt,
        LoginContext loginContext
) {
}

