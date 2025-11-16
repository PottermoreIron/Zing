package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidPhone;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: Pot
 * @created: 2025/11/16 22:05
 * @description: 手机号验证器
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
                ? ValidationUtils.isPhone(phone)
                : ValidationUtils.isPhone(phone, pattern);
    }
}
