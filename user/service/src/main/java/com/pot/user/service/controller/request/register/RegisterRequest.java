package com.pot.user.service.controller.request.register;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
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
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PhoneCodeRegisterRequest.class, name = "4"),
        @JsonSubTypes.Type(value = EmailCodeRegisterRequest.class, name = "5")
})
public abstract class RegisterRequest {
    @Min(value = 1, message = "值不能小于1")
    @Max(value = 5, message = "值不能大于5")
    int type;
}
