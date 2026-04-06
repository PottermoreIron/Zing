package com.pot.auth.interfaces.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.pot.auth.domain.shared.valueobject.UserDomain;

import java.io.IOException;

/**
 * Deserializes user domains and falls back to MEMBER when absent.
 */
public class UserDomainDeserializer extends JsonDeserializer<UserDomain> {

    private static final UserDomain DEFAULT_USER_DOMAIN = UserDomain.MEMBER;

    @Override
    public UserDomain deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        if (value == null || value.isBlank()) {
            return DEFAULT_USER_DOMAIN;
        }

        return UserDomain.fromCode(value);
    }

    @Override
    public UserDomain getNullValue(DeserializationContext ctxt) {
        return DEFAULT_USER_DOMAIN;
    }
}
