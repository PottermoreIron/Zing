package com.pot.auth.interfaces.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.pot.auth.domain.shared.valueobject.UserDomain;

import java.io.IOException;

/**
 * UserDomain 反序列化器
 *
 * <p>
 * 当前端未传递 userDomain 字段时，自动设置为默认值 MEMBER
 *
 * @author pot
 * @since 2025-11-29
 */
public class UserDomainDeserializer extends JsonDeserializer<UserDomain> {

    /**
     * 默认用户域为 MEMBER（C端用户）
     */
    private static final UserDomain DEFAULT_USER_DOMAIN = UserDomain.MEMBER;

    @Override
    public UserDomain deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        // 如果值为空，返回默认值
        if (value == null || value.isBlank()) {
            return DEFAULT_USER_DOMAIN;
        }

        // 否则按正常流程解析
        return UserDomain.fromCode(value);
    }

    @Override
    public UserDomain getNullValue(DeserializationContext ctxt) {
        // 当字段为 null 时，返回默认值
        return DEFAULT_USER_DOMAIN;
    }
}
