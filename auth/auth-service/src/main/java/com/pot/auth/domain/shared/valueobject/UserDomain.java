package com.pot.auth.domain.shared.valueobject;

import lombok.Getter;

/**
 * 用户域枚举
 *
 * <p>支持多用户域扩展，每个域对应一个独立的用户服务
 * <ul>
 *   <li>MEMBER: 会员域 - C端用户 (member-service)</li>
 *   <li>ADMIN: 后台用户域 - B端员工 (admin-service，预留)</li>
 *   <li>MERCHANT: 商户域 - 商家用户 (merchant-service，未来扩展)</li>
 * </ul>
 *
 * <p>扩展新用户域只需3步：
 * <ol>
 *   <li>在此枚举中添加新域</li>
 *   <li>创建对应的Feign Client (如MerchantServiceClient)</li>
 *   <li>实现UserModulePort接口 (如MerchantModuleAdapter)</li>
 * </ol>
 *
 * @author pot
 * @since 1.0.0
 */
@Getter
public enum UserDomain {

    /**
     * 会员域 - C端用户
     */
    MEMBER("member", "会员"),

    /**
     * 后台用户域 - B端员工 (预留)
     */
    ADMIN("admin", "后台用户"),

    /**
     * 商户域 - 商家用户 (未来扩展)
     */
    MERCHANT("merchant", "商户");

    private final String code;
    private final String description;

    UserDomain(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取用户域枚举
     *
     * @param code 用户域编码
     * @return 用户域枚举
     * @throws IllegalArgumentException 如果编码无效
     */
    public static UserDomain fromCode(String code) {
        for (UserDomain domain : UserDomain.values()) {
            if (domain.getCode().equalsIgnoreCase(code)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("无效的用户域编码: " + code);
    }
}

