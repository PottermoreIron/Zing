package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 会员数据传输对象（对外 RPC 契约）
 *
 * <p>
 * 用于跨服务传输会员核心信息，字段设计遵循最小化暴露原则。
 * Profile 扩展信息请使用 {@link MemberProfileDTO}。
 *
 * @author Pot
 * @since 2026-03-18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会员唯一ID */
    private Long memberId;

    /** 昵称（显示名） */
    private String nickname;

    /** 邮箱地址 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatarUrl;

    /** 账户状态：ACTIVE / LOCKED / DISABLED */
    private String status;

    /** 拥有的角色代码集合 */
    private Set<String> roleCodes;

    /** 拥有的权限代码集合 */
    private Set<String> permissionCodes;

    /** 注册时间（毫秒时间戳） */
    private Long gmtCreatedAt;

    /** 最后登录时间（毫秒时间戳） */
    private Long gmtLastLoginAt;
}
