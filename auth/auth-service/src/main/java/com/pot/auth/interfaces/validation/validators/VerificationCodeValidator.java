package com.pot.auth.interfaces.validation.validators;

import com.pot.auth.interfaces.validation.annotations.ValidVerificationCode;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean Validation adapter for verification codes.
 */
public class VerificationCodeValidator implements ConstraintValidator<ValidVerificationCode, String> {

    private String pattern;

    @Override
    public void initialize(ValidVerificationCode constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String verificationCode, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(verificationCode)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidVerificationCode(verificationCode)
                : ValidationUtils.isValidVerificationCode(verificationCode, pattern);
    }
}
