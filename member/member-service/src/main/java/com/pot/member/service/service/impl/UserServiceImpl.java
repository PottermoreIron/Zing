package com.pot.member.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.member.service.entity.ThirdPartyConnection;
import com.pot.member.service.entity.User;
import com.pot.member.service.enums.OAuth2Enum;
import com.pot.member.service.mapper.UserMapper;
import com.pot.member.service.service.ThirdPartyConnectionService;
import com.pot.member.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author pot
 * @since 2025-02-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final ThirdPartyConnectionService thirdPartyConnectionService;

    @Override
    public User findByThirdPartyUserIdAndType(String thirdPartyUserId, OAuth2Enum type) {
        ThirdPartyConnection connection = thirdPartyConnectionService.lambdaQuery()
                .eq(ThirdPartyConnection::getThirdPartyUserId, thirdPartyUserId)
                .eq(ThirdPartyConnection::getPlatformType, type.getName())
                .one();

        if (connection == null) {
            return null;
        }

        Long uid = connection.getUid();
        return this.lambdaQuery()
                .eq(User::getUid, uid)
                .one();
    }
}
