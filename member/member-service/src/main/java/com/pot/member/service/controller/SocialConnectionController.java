package com.pot.member.service.controller;

import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.service.converter.SocialConnectionConverter;
import com.pot.member.service.entity.SocialConnection;
import com.pot.member.service.service.SocialConnectionsService;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社交账号连接控制器（Facade实现）
 * <p>
 * 实现MemberFacade中的社交账号相关接口，供Auth服务等外部服务调用
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@Slf4j
@RestController
@RequestMapping("/member/social-connections")
@RequiredArgsConstructor
public class SocialConnectionController {

    private final SocialConnectionsService socialConnectionsService;
    private final SocialConnectionConverter socialConnectionConverter;

    /**
     * 绑定社交账号
     *
     * @param request 绑定请求
     * @return 绑定后的连接信息
     */
    @PostMapping("/bind")
    public R<SocialConnectionDTO> bindSocialAccount(@Valid @RequestBody BindSocialAccountRequest request) {
        log.info("[SocialConnectionController] 绑定社交账号, memberId={}, provider={}",
                request.getMemberId(), request.getProvider());

        try {
            SocialConnection connection = socialConnectionsService.createConnection(request);
            SocialConnectionDTO dto = socialConnectionConverter.toDTO(connection);

            log.info("[SocialConnectionController] 绑定成功, id={}, memberId={}, provider={}",
                    connection.getId(), request.getMemberId(), request.getProvider());

            return R.success(dto, "绑定成功");

        } catch (Exception e) {
            log.error("[SocialConnectionController] 绑定失败, memberId={}, provider={}",
                    request.getMemberId(), request.getProvider(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 解绑社交账号
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @DeleteMapping("/unbind")
    public R<Void> unbindSocialAccount(@RequestParam("memberId") Long memberId,
                                       @RequestParam("provider") String provider) {
        log.info("[SocialConnectionController] 解绑社交账号, memberId={}, provider={}", memberId, provider);

        try {
            socialConnectionsService.removeConnection(memberId, provider);

            log.info("[SocialConnectionController] 解绑成功, memberId={}, provider={}", memberId, provider);

            return R.success(null, "解绑成功");

        } catch (Exception e) {
            log.error("[SocialConnectionController] 解绑失败, memberId={}, provider={}", memberId, provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取用户的所有社交账号连接
     *
     * @param memberId 用户ID
     * @return 连接列表
     */
    @GetMapping("/list")
    public R<List<SocialConnectionDTO>> getSocialConnections(@RequestParam("memberId") Long memberId) {
        log.debug("[SocialConnectionController] 获取社交连接列表, memberId={}", memberId);

        try {
            List<SocialConnection> connections = socialConnectionsService.listByMemberId(memberId);
            List<SocialConnectionDTO> dtos = connections.stream()
                    .map(socialConnectionConverter::toDTO)
                    .collect(Collectors.toList());

            log.debug("[SocialConnectionController] 查询成功, memberId={}, count={}", memberId, dtos.size());

            return R.success(dtos);

        } catch (Exception e) {
            log.error("[SocialConnectionController] 查询失败, memberId={}", memberId, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取特定平台的连接信息
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 连接信息
     */
    @GetMapping("/get")
    public R<SocialConnectionDTO> getSocialConnection(@RequestParam("memberId") Long memberId,
                                                      @RequestParam("provider") String provider) {
        log.debug("[SocialConnectionController] 获取社交连接, memberId={}, provider={}", memberId, provider);

        try {
            SocialConnection connection = socialConnectionsService.getByMemberIdAndProvider(memberId, provider);
            if (connection == null) {
                return R.success(null, "未找到绑定关系");
            }

            SocialConnectionDTO dto = socialConnectionConverter.toDTO(connection);

            log.debug("[SocialConnectionController] 查询成功, memberId={}, provider={}", memberId, provider);

            return R.success(dto);

        } catch (Exception e) {
            log.error("[SocialConnectionController] 查询失败, memberId={}, provider={}", memberId, provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 检查第三方账号是否已被绑定
     *
     * @param provider         平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return true-已绑定，false-未绑定
     */
    @GetMapping("/check-bound")
    public R<Boolean> isSocialAccountBound(@RequestParam("provider") String provider,
                                           @RequestParam("providerMemberId") String providerMemberId) {
        log.debug("[SocialConnectionController] 检查账号是否已绑定, provider={}, providerMemberId=***",
                provider);

        try {
            SocialConnection connection = socialConnectionsService
                    .getByProviderAndProviderId(provider, providerMemberId);
            boolean isBound = connection != null;

            log.debug("[SocialConnectionController] 检查完成, provider={}, isBound={}", provider, isBound);

            return R.success(isBound);

        } catch (Exception e) {
            log.error("[SocialConnectionController] 检查失败, provider={}", provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据第三方账号查询用户ID
     *
     * @param provider         平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return 用户ID
     */
    @GetMapping("/get-member-id")
    public R<Long> getMemberIdBySocialAccount(@RequestParam("provider") String provider,
                                              @RequestParam("providerMemberId") String providerMemberId) {
        log.debug("[SocialConnectionController] 根据第三方账号查询用户ID, provider={}", provider);

        try {
            SocialConnection connection = socialConnectionsService
                    .getByProviderAndProviderId(provider, providerMemberId);

            Long memberId = connection != null ? connection.getMemberId() : null;

            log.debug("[SocialConnectionController] 查询完成, provider={}, memberId={}", provider, memberId);

            return R.success(memberId);

        } catch (Exception e) {
            log.error("[SocialConnectionController] 查询失败, provider={}", provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新社交账号令牌
     *
     * @param memberId     用户ID
     * @param provider     平台提供商
     * @param accessToken  新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresAt    过期时间
     * @return 操作结果
     */
    @PutMapping("/update-tokens")
    public R<Void> updateSocialAccountTokens(@RequestParam("memberId") Long memberId,
                                             @RequestParam("provider") String provider,
                                             @RequestParam("accessToken") String accessToken,
                                             @RequestParam(value = "refreshToken", required = false) String refreshToken,
                                             @RequestParam(value = "expiresAt", required = false) Long expiresAt) {
        log.info("[SocialConnectionController] 更新令牌, memberId={}, provider={}", memberId, provider);

        try {
            socialConnectionsService.updateTokens(memberId, provider, accessToken, refreshToken, expiresAt);

            log.info("[SocialConnectionController] 更新令牌成功, memberId={}, provider={}", memberId, provider);

            return R.success(null, "更新成功");

        } catch (Exception e) {
            log.error("[SocialConnectionController] 更新令牌失败, memberId={}, provider={}", memberId, provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 设置主社交账号
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @PutMapping("/set-primary")
    public R<Void> setPrimarySocialAccount(@RequestParam("memberId") Long memberId,
                                           @RequestParam("provider") String provider) {
        log.info("[SocialConnectionController] 设置主账号, memberId={}, provider={}", memberId, provider);

        try {
            socialConnectionsService.setPrimary(memberId, provider);

            log.info("[SocialConnectionController] 设置主账号成功, memberId={}, provider={}", memberId, provider);

            return R.success(null, "设置成功");

        } catch (Exception e) {
            log.error("[SocialConnectionController] 设置主账号失败, memberId={}, provider={}", memberId, provider, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量获取用户的社交连接信息
     *
     * @param memberIds 用户ID列表
     * @return 用户ID到社交连接列表的映射
     */
    @PostMapping("/batch-get")
    public R<Map<Long, List<SocialConnectionDTO>>> batchGetSocialConnections(@RequestBody List<Long> memberIds) {
        log.debug("[SocialConnectionController] 批量获取社交连接, count={}", memberIds.size());

        try {
            Map<Long, List<SocialConnection>> connectionsMap =
                    socialConnectionsService.batchGetByMemberIds(memberIds);

            Map<Long, List<SocialConnectionDTO>> result = connectionsMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(socialConnectionConverter::toDTO)
                                    .collect(Collectors.toList())
                    ));

            log.debug("[SocialConnectionController] 批量获取成功, count={}", result.size());

            return R.success(result);

        } catch (Exception e) {
            log.error("[SocialConnectionController] 批量获取失败", e);
            return R.fail(e.getMessage());
        }
    }
}

