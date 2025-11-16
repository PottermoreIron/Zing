package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidIdentifier;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: Pot
 * @created: 2025/11/16 22:58
 * @description: 标识符验证器
 */
public class IdentifierValidator implements ConstraintValidator<ValidIdentifier, String> {

    private String pattern;

    @Override
    public void initialize(ValidIdentifier constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String identifier, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(identifier)) {
            return true;
        }
        if (StringUtils.isBlank(pattern)) {
            return ValidationUtils.isValidIdentifier(identifier);
        } else {
            return ValidationUtils.isValidIdentifier(identifier, pattern);
        }
    }
}