package com.pot.zing.framework.starter.id.service;

/**
 * @author: Pot
 * @created: 2025/10/18 23:23
 * @description: 自定义分布式ID生成器接口
 */
public interface IdGenerator {

    /**
     * 生成下一个ID
     *
     * @param bizType 业务类型
     * @return 生成的ID
     */
    Long nextId(String bizType);

    /**
     * 获取生成器类型
     */
    String getType();
}