package com.pot.common.id;

/**
 * @author: Pot
 * @created: 2025/8/17 21:33
 * @description: id接口类
 */
public interface IdService {
    /**
     * 获取下一个ID
     *
     * @param bizType 业务类型
     * @return 下一个ID
     */
    Long getNextId(String bizType);
}
