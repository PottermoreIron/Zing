package com.pot.member.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.member.service.entity.Member;
import com.pot.member.service.mapper.MemberMapper;
import com.pot.member.service.service.MemberService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会员基础信息表 服务实现类
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

}
