package com.pot.member.facade.api;

import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "member-service", path = "/member/social-connections")
public interface SocialConnectionFacade {

        @PostMapping("/bind")
    R<SocialConnectionDTO> bindSocialAccount(@Valid @RequestBody BindSocialAccountRequest request);

        @DeleteMapping("/unbind")
    R<Void> unbindSocialAccount(@RequestParam("memberId") Long memberId,
                                @RequestParam("provider") String provider);

        @GetMapping("/list")
    R<List<SocialConnectionDTO>> getSocialConnections(@RequestParam("memberId") Long memberId);

        @GetMapping("/get")
    R<SocialConnectionDTO> getSocialConnection(@RequestParam("memberId") Long memberId,
                                               @RequestParam("provider") String provider);

        @GetMapping("/check-bound")
    R<Boolean> isSocialAccountBound(@RequestParam("provider") String provider,
                                    @RequestParam("providerMemberId") String providerMemberId);

        @GetMapping("/get-member-id")
    R<Long> getMemberIdBySocialAccount(@RequestParam("provider") String provider,
                                       @RequestParam("providerMemberId") String providerMemberId);

        @PutMapping("/update-tokens")
    R<Void> updateSocialAccountTokens(@RequestParam("memberId") Long memberId,
                                      @RequestParam("provider") String provider,
                                      @RequestParam("accessToken") String accessToken,
                                      @RequestParam(value = "refreshToken", required = false) String refreshToken,
                                      @RequestParam(value = "expiresAt", required = false) Long expiresAt);

        @PutMapping("/set-primary")
    R<Void> setPrimarySocialAccount(@RequestParam("memberId") Long memberId,
                                    @RequestParam("provider") String provider);

        @PostMapping("/batch-get")
    R<Map<Long, List<SocialConnectionDTO>>> batchGetSocialConnections(@RequestBody List<Long> memberIds);
}

