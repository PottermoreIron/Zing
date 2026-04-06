package com.pot.im.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.im.service.entity.UserConversation;
import com.pot.im.service.mapper.UserConversationMapper;
import com.pot.im.service.service.UserConversationService;
import org.springframework.stereotype.Service;

@Service
public class UserConversationServiceImpl extends ServiceImpl<UserConversationMapper, UserConversation> implements UserConversationService {

}
