package com.pot.user.service.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/3/10 22:54
 * @description: 用户注册
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @Min(value = 1, message = "值不能小于1")
    @Max(value = 5, message = "值不能大于5")
    int type;
}
