package com.pot.member.service.domain.port;

/**
 * 密码加密 Port（出站端口）
 *
 * <p>
 * 领域层通过此接口进行密码的加密与校验，不依赖 Spring Security。
 * 基础设施层提供 {@code BCryptPasswordEncoderAdapter} 实现。
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface PasswordEncoder {

    /**
     * 将明文密码编码为哈希值
     */
    String encode(String rawPassword);

    /**
     * 校验明文密码与哈希值是否匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
}
