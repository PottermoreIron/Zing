package com.pot.user.service.service.impl;

import com.pot.user.service.entity.User;
import com.pot.user.service.mapper.UserMapper;
import com.pot.user.service.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
