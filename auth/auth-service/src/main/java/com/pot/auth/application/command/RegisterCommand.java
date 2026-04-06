package com.pot.auth.application.command;

import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

public interface RegisterCommand {

    RegisterType registerType();

    UserDomain userDomain();

    default String nickname() {
        return null;
    }

    default String email() {
        return null;
    }

    default String phone() {
        return null;
    }

    default String password() {
        return null;
    }

    default String verificationCode() {
        return null;
    }

    default String code() {
        return null;
    }

    default String state() {
        return null;
    }

    default String oauth2ProviderCode() {
        return null;
    }
}