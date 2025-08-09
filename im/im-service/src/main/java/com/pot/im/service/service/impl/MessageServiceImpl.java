package com.pot.im.service.service.impl;

import com.pot.im.service.entity.Message;
import com.pot.im.service.mapper.MessageMapper;
import com.pot.im.service.service.MessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息表 服务实现类
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

}
