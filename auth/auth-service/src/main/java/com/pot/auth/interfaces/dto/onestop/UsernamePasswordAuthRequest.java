package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.validation.annotations.ValidPassword;
import com.pot.auth.interfaces.validation.annotations.ValidUsername;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * One-stop authentication request for nickname and password.
 */
public record UsernamePasswordAuthRequest(
        @NotNull(message = "Auth type must not be null") @JsonProperty("authType") AuthType authType,

        @ValidUsername String nickname,

        @ValidPassword String password,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {
}
