package com.pot.user.service.annotations.validators;


import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.annotations.IsMobile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author: Pot
 * @created: 2025/3/16 23:10
 * @description: 手机号码校验的验证类
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return ValidationUtils.isPhone(s);
    }
}
