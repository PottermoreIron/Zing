package com.pot.auth.service.utils;

import com.pot.auth.service.dto.response.AuthUserInfoVO;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.util.BeanUtils;

/**
 * @author: Pot
 * @created: 2025/10/19 22:19
 * @description: 转换工具类
 */
public class ConvertUtils {

    public static AuthUserInfoVO toUserInfoVO(MemberDTO memberDTO) {
        if (memberDTO == null) {
            return null;
        }
        return BeanUtils.convert(memberDTO, AuthUserInfoVO.class);
    }
}
