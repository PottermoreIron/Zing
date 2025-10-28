package com.pot.member.facade.api;

import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: Pot
 * @created: 2025/8/31 21:28
 * @description: member rpc 接口
 */
@FeignClient(name = "member-service", path = "/member")
public interface MemberFacade {
    @GetMapping("/sayHello")
    String sayHello(@RequestParam("name") String name);


    @PostMapping("/create")
    R<MemberDTO> createMember(@RequestBody CreateMemberRequest request);

    @GetMapping("/checkEmailExists")
    R<Boolean> checkEmailExists(@RequestParam("email") String email);

    @GetMapping("/checkPhoneExists")
    R<Boolean> checkPhoneExists(@RequestParam("phone") String phone);

    @GetMapping("/getByUsername")
    R<MemberDTO> getMemberByUsername(@RequestParam("username") String username);

    @GetMapping("/getByEmail")
    R<MemberDTO> getMemberByEmail(@RequestParam("email") String email);

    @GetMapping("/getByPhone")
    R<MemberDTO> getMemberByPhone(@RequestParam("phone") String phone);

    @GetMapping("/getById")
    R<MemberDTO> getMemberById(@RequestParam("memberId") Long memberId);

    /**
     * 根据OAuth2信息查询用户
     *
     * @param provider OAuth2提供商
     * @param openId   OAuth2用户ID
     * @return 用户信息
     */
    @GetMapping("/getByOAuth2")
    R<MemberDTO> getMemberByOAuth2(@RequestParam("provider") String provider,
                                   @RequestParam("openId") String openId);

    /**
     * 从OAuth2信息创建新用户
     *
     * @param provider  OAuth2提供商
     * @param openId    OAuth2用户ID
     * @param email     邮箱
     * @param nickname  昵称
     * @param avatarUrl 头像URL
     * @return 创建的用户信息
     */
    @PostMapping("/createFromOAuth2")
    R<MemberDTO> createMemberFromOAuth2(@RequestParam("provider") String provider,
                                        @RequestParam("openId") String openId,
                                        @RequestParam(value = "email", required = false) String email,
                                        @RequestParam(value = "nickname", required = false) String nickname,
                                        @RequestParam(value = "avatarUrl", required = false) String avatarUrl);

    /**
     * 根据微信UnionID查询用户（用于账号统一）
     *
     * @param unionId 微信UnionID
     * @return 用户信息
     */
    @GetMapping("/getByUnionId")
    R<MemberDTO> getMemberByUnionId(@RequestParam("unionId") String unionId);

    /**
     * 绑定OAuth2账号到已有用户
     *
     * @param memberId 用户ID
     * @param provider OAuth2提供商
     * @param openId   OAuth2用户ID
     * @return 绑定结果
     */
    @PostMapping("/bindOAuth2Account")
    R<Void> bindOAuth2Account(@RequestParam("memberId") Long memberId,
                              @RequestParam("provider") String provider,
                              @RequestParam("openId") String openId);

    /**
     * 更新用户的微信UnionID
     *
     * @param memberId 用户ID
     * @param unionId  微信UnionID
     * @return 更新结果
     */
    @PostMapping("/updateUnionId")
    R<Void> updateUnionId(@RequestParam("memberId") Long memberId,
                          @RequestParam("unionId") String unionId);
}
