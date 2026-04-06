package com.pot.auth.domain.port;

import com.pot.auth.domain.wechat.entity.WeChatUserInfo;

public interface WeChatPort {

        WeChatUserInfo getUserInfo(String code, String state);

        String refreshAccessToken(String refreshToken);

        boolean validateAccessToken(String accessToken);
}
