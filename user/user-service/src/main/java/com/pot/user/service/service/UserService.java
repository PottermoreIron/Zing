package com.pot.user.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.OAuth2Enum;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author pot
 * @since 2025-02-25
 */
public interface UserService extends IService<User> {
    User findByThirdPartyUserIdAndType(String thirdPartyUserId, OAuth2Enum type);
}
