package com.pot.user.service.controller.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/3/19 23:40
 * @description: 发送验证码请求
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
        @JsonSubTypes.Type(value = SendPhoneCodeRequest.class, name = "1"),
        @JsonSubTypes.Type(value = SendEmailCodeRequest.class, name = "2")
})
public abstract class SendCodeRequest {
    @Min(value = 1, message = "值不能小于1")
    @Max(value = 5, message = "值不能大于5")
    int type;
}
