package com.pot.member.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pot.member.service.entity.Member;
import com.pot.member.service.mapper.MemberMapper;
import com.pot.member.service.service.MemberAuthInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 内部认证服务实现
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthInternalServiceImpl implements MemberAuthInternalService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean verifyPassword(String identifier, String password) {
        // 1. 查找用户
        Member member = findMemberByIdentifier(identifier);

        if (member == null) {
            log.debug("[密码验证] 用户不存在: identifier={}", identifier);
            return false;
        }

        // 2. 检查账户状态
        if (member.getStatus() != null && "2".equals(member.getStatus())) {
            log.warn("[密码验证] 账户已锁定: identifier={}, userId={}", identifier, member.getMemberId());
            return false;
        }

        // 3. 验证密码
        String passwordHash = member.getPasswordHash();
        if (passwordHash == null || passwordHash.isEmpty()) {
            log.warn("[密码验证] 用户未设置密码: identifier={}, userId={}", identifier, member.getMemberId());
            return false;
        }

        boolean matches = passwordEncoder.matches(password, passwordHash);

        if (matches) {
            log.info("[密码验证] 验证成功: identifier={}, userId={}", identifier, member.getMemberId());
        } else {
            log.warn("[密码验证] 验证失败: identifier={}, userId={}", identifier, member.getMemberId());
        }

        return matches;
    }

    @Override
    public void recordLoginAttempt(String userId, Boolean success, String ip) {
        log.info("[登录追踪] userId={}, success={}, ip={}", userId, success, ip);

        // TODO: 实现登录尝试记录
        // 1. 记录到登录历史表
        // 2. 如果失败次数过多，自动锁定账户
        // 3. 可以使用Redis记录失败次数，过期时间设置为15分钟

        // 示例：连续5次失败则锁定账户30分钟
        // String key = "login:failed:" + userId;
        // Long failedCount = redisTemplate.opsForValue().increment(key);
        // if (failedCount == 1) {
        // redisTemplate.expire(key, 15, TimeUnit.MINUTES);
        // }
        // if (failedCount >= 5) {
        // lockAccount(userId);
        // }
    }

    @Override
    public void lockAccount(String userId) {
        log.info("[账户锁定] userId={}", userId);

        LambdaUpdateWrapper<Member> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Member::getMemberId, Long.parseLong(userId))
                .set(Member::getStatus, "2"); // 2=已锁定

        int updated = memberMapper.update(null, updateWrapper);

        if (updated > 0) {
            log.info("[账户锁定] 锁定成功: userId={}", userId);
        } else {
            log.warn("[账户锁定] 锁定失败，用户不存在: userId={}", userId);
        }
    }

    @Override
    public void unlockAccount(String userId) {
        log.info("[账户解锁] userId={}", userId);

        LambdaUpdateWrapper<Member> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Member::getMemberId, Long.parseLong(userId))
                .set(Member::getStatus, "1"); // 1=正常

        int updated = memberMapper.update(null, updateWrapper);

        if (updated > 0) {
            log.info("[账户解锁] 解锁成功: userId={}", userId);
        } else {
            log.warn("[账户解锁] 解锁失败，用户不存在: userId={}", userId);
        }
    }

    /**
     * 根据标识符查找用户（支持用户名/邮箱/手机号）
     */
    private Member findMemberByIdentifier(String identifier) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq(Member::getNickname, identifier)
                .or().eq(Member::getEmail, identifier)
                .or().eq(Member::getPhone, identifier));
        queryWrapper.eq(Member::getGmtDeletedAt, null); // 未删除

        return memberMapper.selectOne(queryWrapper);
    }
}
