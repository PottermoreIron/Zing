package com.pot.member.service.application.command;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * 更新会员资料命令
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileCommand {

    private Long memberId;

    @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]*$", message = "用户名只能包含中文、英文、数字、下划线和横线")
    private String username;

    @URL(message = "头像URL格式不正确")
    @Size(max = 500, message = "头像URL不能超过500个字符")
    private String avatar;

    @Size(max = 200, message = "个人简介不能超过200个字符")
    private String bio;
}
