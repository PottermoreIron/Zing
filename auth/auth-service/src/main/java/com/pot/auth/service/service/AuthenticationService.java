package com.pot.auth.service.service;

import com.pot.auth.service.dto.request.LoginRequest;
import com.pot.auth.service.dto.request.RegisterRequest;
import com.pot.auth.service.dto.response.TokenResponse;
import com.pot.auth.service.security.MemberUserDetailsService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 * <p>
 * 处理用户登录、注册、Token刷新等认证相关业务
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final MemberFacade memberFacade;
    private final MemberUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenStore jwtTokenStore;

    /**
     * 用户登录
     */
    public TokenResponse login(LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());

        try {
            // 加载用户信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            SecurityUser user = (SecurityUser) userDetails;

            // 验证密码
            if (!SecurityUtils.matchesPassword(request.getPassword(), user.getPassword())) {
                log.warn("密码错误: username={}", request.getUsername());
                throw new BadCredentialsException("用户名或密码错误");
            }

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 存储RefreshToken
            long refreshTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getRefreshTokenValidity();
            jwtTokenStore.storeRefreshToken(user.getUserId(), refreshToken, refreshTokenValidity);

            // 记录在线用户
            long accessTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();
            jwtTokenStore.recordOnlineUser(user.getUserId(), accessToken, accessTokenValidity);

            log.info("用户登录成功: userId={}, username={}", user.getUserId(), user.getUsername());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenValidity / 1000)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .build();

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录失败", e);
            throw new BusinessException("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse register(RegisterRequest request) {
        log.info("用户注册: email={}, phone={}", request.getEmail(), request.getPhone());

        try {
            // 检查邮箱是否已存在
            if (request.getEmail() != null) {
                R<Boolean> emailExists = memberFacade.checkEmailExists(request.getEmail());
                if (emailExists != null && emailExists.isSuccess() && Boolean.TRUE.equals(emailExists.getData())) {
                    throw new BusinessException("邮箱已被注册");
                }
            }

            // 检查手机号是否已存在
            if (request.getPhone() != null) {
                R<Boolean> phoneExists = memberFacade.checkPhoneExists(request.getPhone());
                if (phoneExists != null && phoneExists.isSuccess() && Boolean.TRUE.equals(phoneExists.getData())) {
                    throw new BusinessException("手机号已被注册");
                }
            }

            // 加密密码
            String encodedPassword = SecurityUtils.encodePassword(request.getPassword());

            // 创建会员
            CreateMemberRequest createRequest = CreateMemberRequest.builder()
                    .nickname(request.getNickname())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .password(encodedPassword)
                    .build();

            R<MemberDTO> createResult = memberFacade.createMember(createRequest);
            if (createResult == null || !createResult.isSuccess() || createResult.getData() == null) {
                throw new BusinessException("注册失败");
            }

            MemberDTO member = createResult.getData();
            log.info("用户注册成功: memberId={}, email={}", member.getMemberId(), member.getEmail());

            // 自动登录
            UserDetails userDetails = userDetailsService.loadUserById(member.getMemberId());
            SecurityUser user = (SecurityUser) userDetails;

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 存储RefreshToken
            long refreshTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getRefreshTokenValidity();
            jwtTokenStore.storeRefreshToken(user.getUserId(), refreshToken, refreshTokenValidity);

            // 记录在线用户
            long accessTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();
            jwtTokenStore.recordOnlineUser(user.getUserId(), accessToken, accessTokenValidity);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenValidity / 1000)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册失败", e);
            throw new BusinessException("注册失败: " + e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    public TokenResponse refreshToken(String refreshToken) {
        log.info("刷新Token");

        try {
            // 验证RefreshToken
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new BusinessException("RefreshToken无效");
            }

            // 验证Token类型
            if (!jwtTokenProvider.validateTokenType(refreshToken, JwtTokenProvider.TokenType.REFRESH)) {
                throw new BusinessException("Token类型错误");
            }

            // 从Token中获取用户ID
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            if (userId == null) {
                throw new BusinessException("无法获取用户信息");
            }

            // 验证RefreshToken是否与存储的一致
            String storedRefreshToken = jwtTokenStore.getRefreshToken(userId);
            if (!refreshToken.equals(storedRefreshToken)) {
                throw new BusinessException("RefreshToken不匹配");
            }

            // 加载用户信息
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            SecurityUser user = (SecurityUser) userDetails;

            // 生成新的AccessToken
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);

            // 记录在线用户
            long accessTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();
            jwtTokenStore.recordOnlineUser(user.getUserId(), newAccessToken, accessTokenValidity);

            log.info("Token刷新成功: userId={}", userId);

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenValidity / 1000)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new BusinessException("刷新Token失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     */
    public void logout(String accessToken) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
            if (userId == null) {
                return;
            }

            // 获取Token过期时间
            long expirationTime = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();

            // 强制下线
            jwtTokenStore.forceLogout(userId, accessToken, expirationTime);

            log.info("用户登出成功: userId={}", userId);

        } catch (Exception e) {
            log.error("登出失败", e);
            throw new BusinessException("登出失败: " + e.getMessage());
        }
    }
}
