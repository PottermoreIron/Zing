package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import lombok.Builder;

/**
 * 创建用户命令（领域层）
 *
 * @author pot
 * @since 1.0.0
 */
@Builder
public record CreateUserCommand(
        String username,
        Email email,
        Phone phone,
        Password password,
        String avatarUrl,
        String firstName,
        String lastName,
        boolean emailVerified,
        String oauth2Provider,
        String oauth2OpenId
) {
}

