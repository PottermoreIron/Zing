package com.pot.auth.service.service.impl;

import com.pot.auth.service.dto.request.login.LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.AuthToken;
import com.pot.auth.service.dto.response.AuthUserInfoVO;
import com.pot.auth.service.service.LoginService;
import com.pot.auth.service.strategy.factory.LoginStrategyFactory;
import com.pot.auth.service.strategy.impl.LoginStrategy;
import com.pot.auth.service.utils.ConvertUtils;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 登录服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final LoginStrategyFactory loginStrategyFactory;
    private final UserTokenUtils userTokenUtils;
    private final JwtUtils jwtUtils;
    private final MemberFacade memberFacade;

    @Override
    @SuppressWarnings("unchecked")
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录请求: type={}", request.getType());

        // 1. 获取对应的登录策略
        LoginStrategy<LoginRequest> strategy = (LoginStrategy<LoginRequest>) loginStrategyFactory.getStrategy(request.getType());

        // 2. 执行登录
        AuthResponse response = strategy.login(request);

        log.info("用户登录成功: type={}, memberId={}",
                request.getType(),
                response.getUserInfo().getMemberId());

        return response;
    }

    @Override
    public void logout(Long userId) {
        log.info("用户退出登录: userId={}", userId);

        // TODO: 实现以下功能
        // 1. 将 Token 加入黑名单（Redis）
        // 2. 清除用户相关缓存
        // 3. 记录退出日志

        log.info("用户退出登录成功: userId={}", userId);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新Token请求");

        try {
            // 1. 解析刷新令牌
            Claims claims = jwtUtils.parseToken(refreshToken);

            // 2. 获取用户ID
            Object userIdObj = claims.get("uid");
            if (userIdObj == null) {
                throw new BusinessException("无效的刷新令牌");
            }

            Long userId = Long.valueOf(userIdObj.toString());

            // 3. 查询用户信息
            R<MemberDTO> result = memberFacade.getMemberById(userId);
            if (!result.isSuccess() || result.getData() == null) {
                throw new BusinessException("用户不存在");
            }

            MemberDTO memberDTO = result.getData();

            // 4. 验证账号状态
            if (!"ACTIVE".equals(memberDTO.getStatus())) {
                throw new BusinessException("账号状态异常，无法刷新Token");
            }

            // 5. 生成新的Token
            AuthUserInfoVO authUserInfoVO = ConvertUtils.toUserInfoVO(memberDTO);
            AuthToken authToken = userTokenUtils.createAccessTokenAndRefreshToken(userId);

            // 6. 构建响应
            AuthResponse response = AuthResponse.builder()
                    .authToken(authToken)
                    .userInfo(authUserInfoVO)
                    .timestamp(System.currentTimeMillis() / 1000)
                    .build();

            log.info("Token刷新成功: userId={}", userId);
            return response;

        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new BusinessException("刷新Token失败: " + e.getMessage());
        }
    }
}
