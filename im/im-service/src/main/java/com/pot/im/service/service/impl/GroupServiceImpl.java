package com.pot.im.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.im.service.entity.Group;
import com.pot.im.service.mapper.GroupMapper;
import com.pot.im.service.service.GroupService;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

}
