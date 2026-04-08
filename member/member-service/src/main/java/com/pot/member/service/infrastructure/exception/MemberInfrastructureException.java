package com.pot.member.service.infrastructure.exception;

/**
 * Signals infrastructure-level failures inside member-service adapters.
 */
public class MemberInfrastructureException extends RuntimeException {

    public MemberInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}