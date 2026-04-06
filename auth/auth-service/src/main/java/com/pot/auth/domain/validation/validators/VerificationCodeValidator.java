package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidVerificationCode;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
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
    public boolean isValid(String verificationCode, jakarta.validation.ConstraintValidatorContext context) {
        if (StringUtils.isBlank(verificationCode)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidVerificationCode(verificationCode)
                : ValidationUtils.isValidVerificationCode(verificationCode, pattern);
    }

}
