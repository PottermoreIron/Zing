package com.pot.im.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.im.service.entity.MessageReadStatus;
import com.pot.im.service.mapper.MessageReadStatusMapper;
import com.pot.im.service.service.MessageReadStatusService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息已读状态表 服务实现类
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Service
public class MessageReadStatusServiceImpl extends ServiceImpl<MessageReadStatusMapper, MessageReadStatus> implements MessageReadStatusService {

}
