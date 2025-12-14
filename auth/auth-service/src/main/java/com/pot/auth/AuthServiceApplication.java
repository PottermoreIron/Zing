package com.pot.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证授权服务
 *
 * <p>架构设计：
 * <ul>
 *   <li>DDD分层架构：领域层、应用层、基础设施层、接口层</li>
 *   <li>六边形架构：通过端口-适配器模式隔离外部依赖</li>
 *   <li>无状态设计：无MySQL数据库，只使用Redis存储临时数据</li>
 *   <li>防腐层：所有外部依赖都通过Port接口访问</li>
 * </ul>
 *
 * <p>核心职责：
 * <ul>
 *   <li>认证管理：多种登录方式、JWT Token签发与验证</li>
 *   <li>注册编排：验证码发送、用户创建流程编排</li>
 *   <li>权限查询：从member-service查询权限并缓存</li>
 *   <li>设备管理：设备列表查询、设备踢出</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pot.auth.infrastructure.client")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

