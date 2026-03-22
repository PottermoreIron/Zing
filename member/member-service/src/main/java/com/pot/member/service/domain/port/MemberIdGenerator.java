package com.pot.member.service.domain.port;

/**
 * 会员ID生成器端口
 *
 * <p>
 * 由基础设施层实现，使用分布式ID生成策略（如 Snowflake），确保全局唯一且不依赖数据库自增。
 * </p>
 *
 * @author Pot
 * @since 2026-03-22
 */
public interface MemberIdGenerator {

    /**
     * 生成新的会员业务ID
     */
    Long nextId();
}
