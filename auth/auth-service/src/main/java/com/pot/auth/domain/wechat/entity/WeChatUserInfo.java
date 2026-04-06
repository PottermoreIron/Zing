package com.pot.auth.domain.wechat.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeChatUserInfo {

        private final String openId;

        private final String unionId;

        private final String nickname;

        private final String avatar;

        private final String country;

        private final String province;

        private final String city;

        private final Integer sex;

        private final String accessToken;

        private final String refreshToken;

        private final Long expiresAt;

        public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (openId != null && openId.length() > 8) {
            return "微信用户_" + openId.substring(openId.length() - 8);
        }
        return "微信用户";
    }

        public String getUniqueId() {
        return (unionId != null && !unionId.isBlank()) ? unionId : openId;
    }

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
