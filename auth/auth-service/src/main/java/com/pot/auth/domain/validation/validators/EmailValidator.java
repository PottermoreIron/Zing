package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean Validation adapter for email values.
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private String pattern;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(email)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidEmail(email)
                : ValidationUtils.isValidEmail(email, pattern);
    }
}
