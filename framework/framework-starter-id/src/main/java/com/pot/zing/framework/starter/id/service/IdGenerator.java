package com.pot.zing.framework.starter.id.service;

/**
 * Strategy interface for distributed ID generators.
 */
public interface IdGenerator {

    /**
     * Generates the next ID for the given business type.
     */
    Long nextId(String bizType);

    /**
     * Returns the generator type identifier.
     */
    String getType();
}