package com.pot.auth.interfaces.validation.validators;

import com.pot.auth.interfaces.validation.annotations.ValidIdentifier;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Bean Validation adapter for generic login identifiers.
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
