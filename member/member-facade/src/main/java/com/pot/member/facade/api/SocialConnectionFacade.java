package com.pot.member.facade.api;

import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社交账号连接Feign客户端
 * <p>
 * 用于Auth服务调用Member服务的社交账号相关接口
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@FeignClient(name = "member-service", path = "/member/social-connections")
public interface SocialConnectionFacade {

    /**
     * 绑定社交账号
     *
     * @param request 绑定请求
     * @return 绑定后的连接信息
     */
    @PostMapping("/bind")
    R<SocialConnectionDTO> bindSocialAccount(@Valid @RequestBody BindSocialAccountRequest request);

    /**
     * 解绑社交账号
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @DeleteMapping("/unbind")
    R<Void> unbindSocialAccount(@RequestParam("memberId") Long memberId,
                                @RequestParam("provider") String provider);

    /**
     * 获取用户的所有社交账号连接
     *
     * @param memberId 用户ID
     * @return 连接列表
     */
    @GetMapping("/list")
    R<List<SocialConnectionDTO>> getSocialConnections(@RequestParam("memberId") Long memberId);

    /**
     * 获取特定平台的连接信息
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 连接信息
     */
    @GetMapping("/get")
    R<SocialConnectionDTO> getSocialConnection(@RequestParam("memberId") Long memberId,
                                               @RequestParam("provider") String provider);

    /**
     * 检查第三方账号是否已被绑定
     *
     * @param provider 平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return true-已绑定，false-未绑定
     */
    @GetMapping("/check-bound")
    R<Boolean> isSocialAccountBound(@RequestParam("provider") String provider,
                                    @RequestParam("providerMemberId") String providerMemberId);

    /**
     * 根据第三方账号查询用户ID
     *
     * @param provider 平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return 用户ID
     */
    @GetMapping("/get-member-id")
    R<Long> getMemberIdBySocialAccount(@RequestParam("provider") String provider,
                                       @RequestParam("providerMemberId") String providerMemberId);

    /**
     * 更新社交账号令牌
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @param accessToken 新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresAt 过期时间
     * @return 操作结果
     */
    @PutMapping("/update-tokens")
    R<Void> updateSocialAccountTokens(@RequestParam("memberId") Long memberId,
                                      @RequestParam("provider") String provider,
                                      @RequestParam("accessToken") String accessToken,
                                      @RequestParam(value = "refreshToken", required = false) String refreshToken,
                                      @RequestParam(value = "expiresAt", required = false) Long expiresAt);

    /**
     * 设置主社交账号
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 操作结果
     */
    @PutMapping("/set-primary")
    R<Void> setPrimarySocialAccount(@RequestParam("memberId") Long memberId,
                                    @RequestParam("provider") String provider);

    /**
     * 批量获取用户的社交连接信息
     *
     * @param memberIds 用户ID列表
     * @return 用户ID到社交连接列表的映射
     */
    @PostMapping("/batch-get")
    R<Map<Long, List<SocialConnectionDTO>>> batchGetSocialConnections(@RequestBody List<Long> memberIds);
}

