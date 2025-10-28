package com.pot.member.service.validator;

import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.member.service.entity.Member;
import com.pot.member.service.service.MemberService;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Member业务校验器
 * <p>
 * 负责Member相关的业务校验逻辑，遵循单一职责原则
 * </p>
 *
 * @author Pot
 * @since 2025-10-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberService memberService;

    /**
     * 校验创建会员请求
     *
     * @param request 创建请求
     * @throws BusinessException 当校验失败时抛出
     */
    public void validateCreateRequest(CreateMemberRequest request) {
        if (request == null) {
            throw new BusinessException("创建请求不能为空");
        }

        // 校验必填字段
        if (StringUtils.isBlank(request.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }

        if (StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException("密码不能为空");
        }

        // 检查邮箱是否已存在
        if (StringUtils.isNotBlank(request.getEmail()) && checkEmailExists(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        // 检查手机号是否已存在
        if (StringUtils.isNotBlank(request.getPhone()) && checkPhoneExists(request.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return true-存在，false-不存在
     */
    public boolean checkEmailExists(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }

        long count = memberService.lambdaQuery()
                .eq(Member::getEmail, email)
                .count();

        log.debug("检查邮箱是否存在: email={}, exists={}", email, count > 0);
        return count > 0;
    }

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return true-存在，false-不存在
     */
    public boolean checkPhoneExists(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }

        long count = memberService.lambdaQuery()
                .eq(Member::getPhone, phone)
                .count();

        log.debug("检查手机号是否存在: phone={}, exists={}", phone, count > 0);
        return count > 0;
    }

    /**
     * 校验会员ID是否有效
     *
     * @param memberId 会员ID
     * @throws BusinessException 当ID无效时抛出
     */
    public void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new BusinessException("会员ID无效");
        }
    }
}


