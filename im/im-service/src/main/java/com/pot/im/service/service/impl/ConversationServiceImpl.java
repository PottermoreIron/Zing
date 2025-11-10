package com.pot.im.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.im.service.entity.Conversation;
import com.pot.im.service.mapper.ConversationMapper;
import com.pot.im.service.service.ConversationService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话表 服务实现类
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

}
