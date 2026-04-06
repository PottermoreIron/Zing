package com.pot.auth.infrastructure.config;

import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.OneStopAuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.RegistrationParameterValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationValidationConfig {

    @Bean
    public ValidationChain<AuthenticationContext> authenticationValidationChain(
            AuthenticationParameterValidator validator) {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(validator);
        return chain;
    }

    @Bean
    public ValidationChain<RegistrationContext> registrationValidationChain(
            RegistrationParameterValidator validator) {
        ValidationChain<RegistrationContext> chain = new ValidationChain<>();
        chain.addHandler(validator);
        return chain;
    }

    @Bean
    public ValidationChain<OneStopAuthContext> oneStopAuthValidationChain(
            OneStopAuthenticationParameterValidator validator) {
        ValidationChain<OneStopAuthContext> chain = new ValidationChain<>();
        chain.addHandler(validator);
        return chain;
    }
}