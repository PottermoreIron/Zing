package com.pot.member.service.facade.impl;

import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.member.service.converter.MemberConverter;
import com.pot.member.service.entity.Member;
import com.pot.member.service.entity.SocialConnection;
import com.pot.member.service.service.MemberService;
import com.pot.member.service.service.SocialConnectionsService;
import com.pot.member.service.validator.MemberValidator;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Member Facade实现类
 * <p>
 * 提供会员服务的RPC接口实现，遵循以下设计原则：
 * 1. 单一职责原则 - 每个方法只负责一个功能
 * 2. 依赖倒置原则 - 依赖抽象而非具体实现
 * 3. 开闭原则 - 对扩展开放，对修改关闭
 * </p>
 *
 * @author Pot
 * @since 2025-08-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberFacadeImpl implements MemberFacade {

    private final MemberService memberService;
    private final MemberValidator memberValidator;
    private final MemberConverter memberConverter;
    private final SocialConnectionsService socialConnectionsService;

    @Override
    public String sayHello(@RequestParam("name") String name) {
        log.debug("调用sayHello方法: name={}", name);
        return "Hello, " + name + "! This is the Member Service.";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<MemberDTO> createMember(@RequestBody CreateMemberRequest request) {
        log.info("开始创建会员: email={}, phone={}, nickname={}",
                request.getEmail(), request.getPhone(), request.getNickname());

        try {
            // 1. 业务校验
            memberValidator.validateCreateRequest(request);

            // 2. 构建实体
            Member member = memberConverter.toEntity(request);

            // 3. 持久化
            boolean saved = memberService.save(member);
            if (!saved) {
                log.error("创建会员失败: 数据库保存失败");
                throw new BusinessException("创建会员失败");
            }

            // 4. 转换为DTO返回
            MemberDTO dto = memberConverter.toDTO(member);

            log.info("会员创建成功: memberId={}, email={}", member.getMemberId(), member.getEmail());
            return R.success(dto);

        } catch (BusinessException e) {
            log.warn("创建会员业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建会员系统异常", e);
            throw new BusinessException("创建会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<Boolean> checkEmailExists(@RequestParam("email") String email) {
        log.debug("检查邮箱是否存在: email={}", email);

        if (StringUtils.isBlank(email)) {
            return R.success(false);
        }

        try {
            boolean exists = memberValidator.checkEmailExists(email);
            return R.success(exists);
        } catch (Exception e) {
            log.error("检查邮箱存在性失败: email={}", email, e);
            throw new BusinessException("检查邮箱失败: " + e.getMessage());
        }
    }

    @Override
    public R<Boolean> checkPhoneExists(@RequestParam("phone") String phone) {
        log.debug("检查手机号是否存在: phone={}", phone);

        if (StringUtils.isBlank(phone)) {
            return R.success(false);
        }

        try {
            boolean exists = memberValidator.checkPhoneExists(phone);
            return R.success(exists);
        } catch (Exception e) {
            log.error("检查手机号存在性失败: phone={}", phone, e);
            throw new BusinessException("检查手机号失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberByUsername(@RequestParam("username") String username) {
        log.info("根据用户名查询会员: username={}", username);

        if (StringUtils.isBlank(username)) {
            log.warn("用户名为空");
            throw new BusinessException("用户名不能为空");
        }

        try {
            Member member = memberService.lambdaQuery()
                    .eq(Member::getNickname, username)
                    .one();

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("查询会员成功: memberId={}, username={}", m.getMemberId(), username);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("会员不存在: username={}", username);
                        return R.fail("会员不存在");
                    });

        } catch (Exception e) {
            log.error("查询会员失败: username={}", username, e);
            throw new BusinessException("查询会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberByEmail(@RequestParam("email") String email) {
        log.info("根据邮箱查询会员: email={}", email);

        if (StringUtils.isBlank(email)) {
            log.warn("邮箱为空");
            throw new BusinessException("邮箱不能为空");
        }

        try {
            Member member = memberService.lambdaQuery()
                    .eq(Member::getEmail, email)
                    .one();

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("查询会员成功: memberId={}, email={}", m.getMemberId(), email);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("会员不存在: email={}", email);
                        return R.fail("会员不存在");
                    });

        } catch (Exception e) {
            log.error("查询会员失败: email={}", email, e);
            throw new BusinessException("查询会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberByPhone(@RequestParam("phone") String phone) {
        log.info("根据手机号查询会员: phone={}", phone);

        if (StringUtils.isBlank(phone)) {
            log.warn("手机号为空");
            throw new BusinessException("手机号不能为空");
        }

        try {
            Member member = memberService.lambdaQuery()
                    .eq(Member::getPhone, phone)
                    .one();

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("查询会员成功: memberId={}, phone={}", m.getMemberId(), phone);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("会员不存在: phone={}", phone);
                        return R.fail("会员不存在");
                    });

        } catch (Exception e) {
            log.error("查询会员失败: phone={}", phone, e);
            throw new BusinessException("查询会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberById(@RequestParam("memberId") Long memberId) {
        log.info("根据ID查询会员: memberId={}", memberId);

        try {
            // 参数校验
            memberValidator.validateMemberId(memberId);

            // 查询会员
            Member member = memberService.getById(memberId);

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("查询会员成功: memberId={}", memberId);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("会员不存在: memberId={}", memberId);
                        return R.fail("会员不存在");
                    });

        } catch (BusinessException e) {
            log.warn("查询会员业务异常: memberId={}, error={}", memberId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("查询会员系统异常: memberId={}", memberId, e);
            throw new BusinessException("查询会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberByOAuth2(@RequestParam("provider") String provider,
                                          @RequestParam("openId") String openId) {
        log.info("根据OAuth2信息查询会员: provider={}, openId={}", provider, openId);

        if (StringUtils.isBlank(provider)) {
            log.warn("OAuth2提供商为空");
            throw new BusinessException("OAuth2提供商不能为空");
        }

        if (StringUtils.isBlank(openId)) {
            log.warn("OAuth2用户ID为空");
            throw new BusinessException("OAuth2用户ID不能为空");
        }

        try {
            // 查询社交连接
            SocialConnection connection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getProvider, provider.toLowerCase())
                    .eq(SocialConnection::getProviderMemberId, openId)
                    .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                    .one();

            if (connection == null) {
                log.info("未找到OAuth2社交连接: provider={}, openId={}", provider, openId);
                return R.fail("未找到OAuth2账号绑定信息");
            }

            // 查询会员信息
            Member member = memberService.getById(connection.getMemberId());

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("通过OAuth2查询会员成功: memberId={}, provider={}, openId={}",
                                m.getMemberId(), provider, openId);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("社交连接关联的会员不存在: memberId={}", connection.getMemberId());
                        return R.fail("关联的会员不存在");
                    });

        } catch (BusinessException e) {
            log.warn("OAuth2查询会员业务异常: provider={}, openId={}, error={}",
                    provider, openId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OAuth2查询会员系统异常: provider={}, openId={}", provider, openId, e);
            throw new BusinessException("OAuth2查询会员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<MemberDTO> createMemberFromOAuth2(@RequestParam("provider") String provider,
                                               @RequestParam("openId") String openId,
                                               @RequestParam(value = "email", required = false) String email,
                                               @RequestParam(value = "nickname", required = false) String nickname,
                                               @RequestParam(value = "avatarUrl", required = false) String avatarUrl) {
        log.info("从OAuth2信息创建会员: provider={}, openId={}, email={}, nickname={}",
                provider, openId, email, nickname);

        // 参数校验
        if (StringUtils.isBlank(provider)) {
            throw new BusinessException("OAuth2提供商不能为空");
        }

        if (StringUtils.isBlank(openId)) {
            throw new BusinessException("OAuth2用户ID不能为空");
        }

        try {
            // 检查是否已存在该OAuth2连接
            SocialConnection existingConnection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getProvider, provider.toLowerCase())
                    .eq(SocialConnection::getProviderMemberId, openId)
                    .one();

            if (existingConnection != null) {
                log.warn("OAuth2连接已存在: provider={}, openId={}", provider, openId);
                throw new BusinessException("该OAuth2账号已绑定");
            }

            // 生成昵称（如果未提供）
            String finalNickname = StringUtils.isNotBlank(nickname)
                    ? nickname
                    : generateNickname(provider, openId);

            // 检查邮箱是否已被使用
            if (StringUtils.isNotBlank(email) && memberValidator.checkEmailExists(email)) {
                log.warn("邮箱已被注册: email={}", email);
                throw new BusinessException("邮箱已被注册，请使用账号绑定功能");
            }

            // 创建会员实体
            Member member = Member.builder()
                    .nickname(finalNickname)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .status(Member.AccountStatus.ACTIVE.getCode())
                    .gender(Member.Gender.UNKNOWN.getCode())
                    .build();

            // 保存会员
            boolean saved = memberService.save(member);
            if (!saved) {
                log.error("创建OAuth2会员失败: 数据库保存失败");
                throw new BusinessException("创建会员失败");
            }

            // 创建社交连接
            SocialConnection connection = SocialConnection.builder()
                    .memberId(member.getMemberId())
                    .provider(provider.toLowerCase())
                    .providerMemberId(openId)
                    .providerUsername(nickname)
                    .providerEmail(email)
                    .isActive(SocialConnection.Status.ACTIVE.getCode())
                    .accessToken("") // 根据实际需求设置
                    .build();

            boolean connectionSaved = socialConnectionsService.save(connection);
            if (!connectionSaved) {
                log.error("创建社交连接失败: memberId={}", member.getMemberId());
                throw new BusinessException("创建社交连接失败");
            }

            MemberDTO dto = memberConverter.toDTO(member);
            log.info("从OAuth2创建会员成功: memberId={}, provider={}, openId={}",
                    member.getMemberId(), provider, openId);

            return R.success(dto);

        } catch (BusinessException e) {
            log.warn("OAuth2创建会员业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OAuth2创建会员系统异常", e);
            throw new BusinessException("OAuth2创建会员失败: " + e.getMessage());
        }
    }

    @Override
    public R<MemberDTO> getMemberByUnionId(@RequestParam("unionId") String unionId) {
        log.info("根据UnionID查询会员: unionId={}", unionId);

        if (StringUtils.isBlank(unionId)) {
            log.warn("UnionID为空");
            throw new BusinessException("UnionID不能为空");
        }

        try {
            // 查询微信社交连接（通过extendJson中的unionId）
            // 注意：这里假设unionId存储在extendJson中，实际项目中建议在表中增加union_id字段
            SocialConnection connection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getProvider, SocialConnection.Provider.WECHAT.getCode())
                    .like(SocialConnection::getExtendJson, unionId)
                    .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                    .one();

            if (connection == null) {
                log.info("未找到UnionID对应的社交连接: unionId={}", unionId);
                return R.fail("未找到对应的会员");
            }

            // 查询会员信息
            Member member = memberService.getById(connection.getMemberId());

            return Optional.ofNullable(member)
                    .map(m -> {
                        log.info("通过UnionID查询会员成功: memberId={}, unionId={}",
                                m.getMemberId(), unionId);
                        return R.success(memberConverter.toDTO(m));
                    })
                    .orElseGet(() -> {
                        log.warn("UnionID关联的会员不存在: memberId={}", connection.getMemberId());
                        return R.fail("关联的会员不存在");
                    });

        } catch (BusinessException e) {
            log.warn("UnionID查询会员业务异常: unionId={}, error={}", unionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("UnionID查询会员系统异常: unionId={}", unionId, e);
            throw new BusinessException("UnionID查询会员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> bindOAuth2Account(@RequestParam("memberId") Long memberId,
                                     @RequestParam("provider") String provider,
                                     @RequestParam("openId") String openId) {
        log.info("绑定OAuth2账号: memberId={}, provider={}, openId={}", memberId, provider, openId);

        // 参数校验
        memberValidator.validateMemberId(memberId);

        if (StringUtils.isBlank(provider)) {
            throw new BusinessException("OAuth2提供商不能为空");
        }

        if (StringUtils.isBlank(openId)) {
            throw new BusinessException("OAuth2用户ID不能为空");
        }

        try {
            // 检查会员是否存在
            Member member = memberService.getById(memberId);
            if (member == null) {
                log.warn("会员不存在: memberId={}", memberId);
                throw new BusinessException("会员不存在");
            }

            // 检查该OAuth2账号是否已被其他用户绑定
            SocialConnection existingConnection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getProvider, provider.toLowerCase())
                    .eq(SocialConnection::getProviderMemberId, openId)
                    .one();

            if (existingConnection != null) {
                if (existingConnection.getMemberId().equals(memberId)) {
                    log.info("OAuth2账号已绑定到当前用户: memberId={}, provider={}", memberId, provider);
                    return R.success();
                } else {
                    log.warn("OAuth2账号已被其他用户绑定: provider={}, openId={}, boundMemberId={}",
                            provider, openId, existingConnection.getMemberId());
                    throw new BusinessException("该OAuth2账号已被其他用户绑定");
                }
            }

            // 检查当前用户是否已绑定该提供商的账号
            SocialConnection userConnection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getMemberId, memberId)
                    .eq(SocialConnection::getProvider, provider.toLowerCase())
                    .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                    .one();

            if (userConnection != null) {
                log.warn("用户已绑定该提供商的其他账号: memberId={}, provider={}, existingOpenId={}",
                        memberId, provider, userConnection.getProviderMemberId());
                throw new BusinessException("您已绑定该平台的其他账号，请先解绑");
            }

            // 创建新的社交连接
            SocialConnection newConnection = SocialConnection.builder()
                    .memberId(memberId)
                    .provider(provider.toLowerCase())
                    .providerMemberId(openId)
                    .isActive(SocialConnection.Status.ACTIVE.getCode())
                    .accessToken("") // 根据实际需求设置
                    .build();

            boolean saved = socialConnectionsService.save(newConnection);
            if (!saved) {
                log.error("绑定OAuth2账号失败: 数据库保存失败");
                throw new BusinessException("绑定OAuth2账号失败");
            }

            log.info("OAuth2账号绑定成功: memberId={}, provider={}, openId={}", memberId, provider, openId);
            return R.success();

        } catch (BusinessException e) {
            log.warn("绑定OAuth2账号业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("绑定OAuth2账号系统异常", e);
            throw new BusinessException("绑定OAuth2账号失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> updateUnionId(@RequestParam("memberId") Long memberId,
                                 @RequestParam("unionId") String unionId) {
        log.info("更新UnionID: memberId={}, unionId={}", memberId, unionId);

        // 参数校验
        memberValidator.validateMemberId(memberId);

        if (StringUtils.isBlank(unionId)) {
            throw new BusinessException("UnionID不能为空");
        }

        try {
            // 检查会员是否存在
            Member member = memberService.getById(memberId);
            if (member == null) {
                log.warn("会员不存在: memberId={}", memberId);
                throw new BusinessException("会员不存在");
            }

            // 查询用户的微信社交连接
            SocialConnection wechatConnection = socialConnectionsService.lambdaQuery()
                    .eq(SocialConnection::getMemberId, memberId)
                    .eq(SocialConnection::getProvider, SocialConnection.Provider.WECHAT.getCode())
                    .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                    .one();

            if (wechatConnection == null) {
                log.warn("未找到用户的微信连接: memberId={}", memberId);
                throw new BusinessException("未找到微信账号绑定信息");
            }

            // 更新extendJson，添加unionId
            // 注意：这里使用简单的JSON拼接，实际项目中建议使用JSON库处理
            String extendJson = wechatConnection.getExtendJson();
            String updatedJson;
            if (StringUtils.isBlank(extendJson) || "{}".equals(extendJson)) {
                updatedJson = String.format("{\"unionId\":\"%s\"}", unionId);
            } else {
                // 简单处理：如果已有extendJson，追加unionId
                updatedJson = extendJson.replaceFirst("}$", String.format(",\"unionId\":\"%s\"}", unionId));
            }

            wechatConnection.setExtendJson(updatedJson);
            boolean updated = socialConnectionsService.updateById(wechatConnection);

            if (!updated) {
                log.error("更新UnionID失败: 数据库更新失败");
                throw new BusinessException("更新UnionID失败");
            }

            log.info("UnionID更新成功: memberId={}, unionId={}", memberId, unionId);
            return R.success();

        } catch (BusinessException e) {
            log.warn("更新UnionID业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新UnionID系统异常", e);
            throw new BusinessException("更新UnionID失败: " + e.getMessage());
        }
    }

    /**
     * 生成默认昵称
     * <p>
     * 为通过OAuth2注册的用户生成默认昵称
     * </p>
     *
     * @param provider OAuth2提供商
     * @param openId   OAuth2用户ID
     * @return 生成的昵称
     */
    private String generateNickname(String provider, String openId) {
        // 截取openId的后8位作为昵称的一部分
        String suffix = openId.length() > 8
                ? openId.substring(openId.length() - 8)
                : openId;

        String providerName;
        try {
            providerName = SocialConnection.Provider.fromCode(provider.toLowerCase()).getDescription();
        } catch (IllegalArgumentException e) {
            providerName = provider;
        }

        return String.format("%s用户_%s", providerName, suffix);
    }
}
