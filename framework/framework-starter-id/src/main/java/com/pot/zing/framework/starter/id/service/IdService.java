package com.pot.zing.framework.starter.id.service;

/**
 * @author: Pot
 * @created: 2025/10/19 20:00
 * @description: 自定义分布式id服务接口类
 */
public interface IdService {
    /**
     * 获取下一个分布式Id
     */
    Long nextId(String bizType);
}
