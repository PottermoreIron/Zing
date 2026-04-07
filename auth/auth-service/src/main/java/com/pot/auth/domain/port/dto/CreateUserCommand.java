package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import lombok.Builder;

@Builder
public record CreateUserCommand(
        String nickname,
        Email email,
        Phone phone,
        Password password,
        String avatarUrl,
        String firstName,
        String lastName,
        String displayName,
        boolean emailVerified,

        String oauth2Provider,

        String oauth2OpenId,

        String weChatOpenId,

        String weChatUnionId) {
}
