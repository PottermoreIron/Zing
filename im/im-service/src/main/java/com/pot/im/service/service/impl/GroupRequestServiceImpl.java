package com.pot.im.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.im.service.entity.GroupRequest;
import com.pot.im.service.mapper.GroupRequestMapper;
import com.pot.im.service.service.GroupRequestService;
import org.springframework.stereotype.Service;

@Service
public class GroupRequestServiceImpl extends ServiceImpl<GroupRequestMapper, GroupRequest> implements GroupRequestService {

}
