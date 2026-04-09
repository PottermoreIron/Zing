package com.pot.auth.interfaces.validation.validators;

import com.pot.auth.interfaces.validation.annotations.ValidPhone;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean Validation adapter for phone values.
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private String pattern;

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(phone)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidPhone(phone)
                : ValidationUtils.isValidPhone(phone, pattern);
    }
}
