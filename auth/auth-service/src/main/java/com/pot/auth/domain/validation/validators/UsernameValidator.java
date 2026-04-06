package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidUsername;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean Validation adapter for nickname values.
 */
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private String pattern;

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isBlank(username)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidNickname(username)
                : ValidationUtils.isValidNickname(username, pattern);
    }
}
