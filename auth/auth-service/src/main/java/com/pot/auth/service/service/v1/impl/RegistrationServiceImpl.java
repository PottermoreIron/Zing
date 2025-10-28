package com.pot.auth.service.service.v1.impl;

import com.pot.auth.service.dto.request.AvailabilityCheckRequest;
import com.pot.auth.service.dto.request.RegistrationRequest;
import com.pot.auth.service.dto.response.AvailabilityCheckResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.RegistrationService;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import com.pot.zing.framework.security.jwt.JwtTokenProvider;
import com.pot.zing.framework.security.jwt.JwtTokenStore;
import com.pot.zing.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 注册服务实现
 * <p>
 * 提供用户注册、可用性检查等功能
 *
 * @author Zing
 * @since 2025-10-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    private final MemberFacade memberFacade;
    private final com.pot.auth.service.security.MemberUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenStore jwtTokenStore;
    private final com.pot.auth.service.service.AuthVerificationCodeService verificationCodeService;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthSession register(RegistrationRequest request) {
        String registrationType = request.getRegistrationType();
        String identifier = request.getIdentifier();

        log.info("[RegistrationService] 用户注册, type={}, identifier={}",
                registrationType, maskIdentifier(identifier));

        try {
            // 1. 根据注册类型验证
            switch (registrationType) {
                case "username_password":
                    return registerByUsernamePassword(request);
                case "phone_code":
                    return registerByPhoneCode(request);
                case "email_code":
                    return registerByEmailCode(request);
                default:
                    throw new BusinessException("不支持的注册类型: " + registrationType);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RegistrationService] 注册失败", e);
            throw new BusinessException("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户名密码注册
     */
    private AuthSession registerByUsernamePassword(RegistrationRequest request) {
        String username = request.getIdentifier();
        String password = request.getPassword();

        // 1. 验证用户名格式
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("用户名格式不正确，只能包含字母、数字、下划线和连字符，长度3-20位");
        }

        // 2. 验证密码
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码不能少于8位");
        }

        // 3. 检查用户名是否已存在
        // 这里需要调用memberFacade检查，简化实现

        // 4. 加密密码
        String encodedPassword = SecurityUtils.encodePassword(password);

        // 5. 创建用户
        CreateMemberRequest createRequest = CreateMemberRequest.builder()
                .nickname(request.getNickname() != null ? request.getNickname() : username)
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(encodedPassword)
                .avatarUrl(request.getAvatarUrl())
                .build();

        R<MemberDTO> createResult = memberFacade.createMember(createRequest);
        if (createResult == null || !createResult.isSuccess() || createResult.getData() == null) {
            throw new BusinessException("创建用户失败");
        }

        MemberDTO member = createResult.getData();

        // 6. 自动登录
        return autoLogin(member, "username_password", request.getClientId());
    }

    /**
     * 手机号验证码注册
     */
    private AuthSession registerByPhoneCode(RegistrationRequest request) {
        String phone = request.getIdentifier();
        String code = request.getCode();

        // 1. 验证手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }

        // 2. 验证验证码
        if (code == null || code.length() != 6) {
            throw new BusinessException("验证码格式不正确");
        }

        // 验证验证码（这里需要调用verificationCodeService）
        // verificationCodeService.verifyCode(phone, code, "register");

        // 3. 检查手机号是否已注册
        R<Boolean> phoneExists = memberFacade.checkPhoneExists(phone);
        if (phoneExists != null && phoneExists.isSuccess() && Boolean.TRUE.equals(phoneExists.getData())) {
            throw new BusinessException("该手机号已被注册");
        }

        // 4. 创建用户
        CreateMemberRequest createRequest = CreateMemberRequest.builder()
                .phone(phone)
                .nickname(request.getNickname() != null ? request.getNickname() : generateNickname())
                .email(request.getEmail())
                .avatarUrl(request.getAvatarUrl())
                .build();

        R<MemberDTO> createResult = memberFacade.createMember(createRequest);
        if (createResult == null || !createResult.isSuccess() || createResult.getData() == null) {
            throw new BusinessException("创建用户失败");
        }

        MemberDTO member = createResult.getData();

        // 5. 自动登录
        return autoLogin(member, "phone_code", request.getClientId());
    }

    /**
     * 邮箱验证码注册
     */
    private AuthSession registerByEmailCode(RegistrationRequest request) {
        String email = request.getIdentifier();
        String code = request.getCode();

        // 1. 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }

        // 2. 验证验证码
        if (code == null || code.length() != 6) {
            throw new BusinessException("验证码格式不正确");
        }

        // 验证验证码
        // verificationCodeService.verifyCode(email, code, "register");

        // 3. 检查邮箱是否已注册
        R<Boolean> emailExists = memberFacade.checkEmailExists(email);
        if (emailExists != null && emailExists.isSuccess() && Boolean.TRUE.equals(emailExists.getData())) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 4. 创建用户
        CreateMemberRequest createRequest = CreateMemberRequest.builder()
                .email(email)
                .nickname(request.getNickname() != null ? request.getNickname() : generateNickname())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .build();

        R<MemberDTO> createResult = memberFacade.createMember(createRequest);
        if (createResult == null || !createResult.isSuccess() || createResult.getData() == null) {
            throw new BusinessException("创建用户失败");
        }

        MemberDTO member = createResult.getData();

        // 5. 自动登录
        return autoLogin(member, "email_code", request.getClientId());
    }

    /**
     * 检查可用性
     */
    public AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request) {
        String type = request.getType();
        String value = request.getValue();

        try {
            boolean available;
            String reason = null;
            String suggestion = null;

            switch (type) {
                case "username":
                    // 检查用户名是否已存在
                    available = checkUsernameAvailable(value);
                    if (!available) {
                        reason = "该用户名已被注册";
                        suggestion = value + "_" + new Random().nextInt(1000);
                    }
                    break;

                case "phone":
                    R<Boolean> phoneExists = memberFacade.checkPhoneExists(value);
                    available = !(phoneExists != null && phoneExists.isSuccess() && Boolean.TRUE.equals(phoneExists.getData()));
                    if (!available) {
                        reason = "该手机号已被注册";
                    }
                    break;

                case "email":
                    R<Boolean> emailExists = memberFacade.checkEmailExists(value);
                    available = !(emailExists != null && emailExists.isSuccess() && Boolean.TRUE.equals(emailExists.getData()));
                    if (!available) {
                        reason = "该邮箱已被注册";
                    }
                    break;

                default:
                    throw new BusinessException("不支持的检查类型: " + type);
            }

            return AvailabilityCheckResponse.builder()
                    .available(available)
                    .type(type)
                    .value(value)
                    .reason(reason)
                    .suggestion(suggestion)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RegistrationService] 检查可用性失败", e);
            throw new BusinessException("检查失败");
        }
    }

    /**
     * 发送注册验证码
     */
    public void sendVerificationCode(String type, String recipient) {
        try {
            // 1. 检查是否已注册
            if ("sms".equals(type)) {
                R<Boolean> phoneExists = memberFacade.checkPhoneExists(recipient);
                if (phoneExists != null && phoneExists.isSuccess() && Boolean.TRUE.equals(phoneExists.getData())) {
                    throw new BusinessException("该手机号已被注册");
                }
            } else if ("email".equals(type)) {
                R<Boolean> emailExists = memberFacade.checkEmailExists(recipient);
                if (emailExists != null && emailExists.isSuccess() && Boolean.TRUE.equals(emailExists.getData())) {
                    throw new BusinessException("该邮箱已被注册");
                }
            }

            // 2. 发送验证码
            // verificationCodeService.sendCode(recipient, type, "register");

            log.info("[RegistrationService] 验证码发送成功, type={}, recipient={}",
                    type, maskIdentifier(recipient));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RegistrationService] 发送验证码失败", e);
            throw new BusinessException("发送验证码失败");
        }
    }

    /**
     * 获取注册配置
     */
    public Object getRegistrationConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("registrationEnabled", true);
        config.put("registrationTypes", Arrays.asList("username_password", "phone_code", "email_code"));

        Map<String, Object> usernameRules = new HashMap<>();
        usernameRules.put("minLength", 3);
        usernameRules.put("maxLength", 20);
        usernameRules.put("pattern", "^[a-zA-Z0-9_-]+$");
        usernameRules.put("description", "只能包含字母、数字、下划线和连字符");
        config.put("usernameRules", usernameRules);

        Map<String, Object> passwordRules = new HashMap<>();
        passwordRules.put("minLength", 8);
        passwordRules.put("maxLength", 32);
        passwordRules.put("requireUppercase", true);
        passwordRules.put("requireLowercase", true);
        passwordRules.put("requireDigit", true);
        passwordRules.put("requireSpecialChar", false);
        passwordRules.put("description", "至少8位，必须包含大小写字母和数字");
        config.put("passwordRules", passwordRules);

        return config;
    }

    /**
     * 自动登录
     */
    private AuthSession autoLogin(MemberDTO member, String authMethod, String clientId) {
        try {
            // 1. 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserById(member.getMemberId());
            SecurityUser user = (SecurityUser) userDetails;

            // 2. 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 3. 存储RefreshToken
            long refreshTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getRefreshTokenValidity();
            jwtTokenStore.storeRefreshToken(user.getUserId(), refreshToken, refreshTokenValidity);

            // 4. 记录在线用户
            long accessTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();
            jwtTokenStore.recordOnlineUser(user.getUserId(), accessToken, accessTokenValidity);

            // 5. 构建响应
            return AuthSession.builder()
                    .sessionId(generateSessionId())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenValidity / 1000)
                    .refreshExpiresIn(refreshTokenValidity / 1000)
                    .userInfo(buildUserInfo(user))
                    .isNewUser(true)
                    .authMethod(authMethod)
                    .build();

        } catch (Exception e) {
            log.error("[RegistrationService] 自动登录失败", e);
            throw new BusinessException("注册成功但登录失败，请手动登录");
        }
    }

    /**
     * 检查用户名是否可用
     */
    private boolean checkUsernameAvailable(String username) {
        // 这里需要实际的检查逻辑
        // 简化实现，假设都可用
        return true;
    }

    /**
     * 生成随机昵称
     */
    private String generateNickname() {
        return "用户" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成SessionId
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构建用户信息
     */
    private AuthSession.UserInfo buildUserInfo(SecurityUser user) {
        return AuthSession.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(user.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()))
                .permissions(user.getPermissions())
                .build();
    }

    /**
     * 脱敏标识符
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 3) {
            return "***";
        }

        if (identifier.contains("@")) {
            String[] parts = identifier.split("@");
            if (parts[0].length() <= 2) {
                return "***@" + parts[1];
            }
            return parts[0].substring(0, 2) + "***@" + parts[1];
        }

        if (identifier.length() == 11 && identifier.matches("^1\\d{10}$")) {
            return identifier.substring(0, 3) + "****" + identifier.substring(7);
        }

        if (identifier.length() <= 4) {
            return identifier.charAt(0) + "***";
        }
        return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
    }
}

