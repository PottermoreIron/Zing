package com.pot.member.service.domain.port;

/**
 * Port for member ID generation.
 *
 * <p>
 * Infrastructure implementations should use a distributed strategy such as
 * Snowflake so IDs
 * stay globally unique without relying on database auto-increment.
 * </p>
 *
 * @author Pot
 * @since 2026-03-22
 */
public interface MemberIdGenerator {

    /**
     * Generate the next member business ID.
     */
    Long nextId();
}
