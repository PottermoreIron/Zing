package com.pot.zing.framework.starter.id.service;

/**
 * Service facade for distributed ID generation.
 */
public interface IdService {
    /**
     * Returns the next distributed ID for the given business type.
     */
    Long nextId(String bizType);
}
