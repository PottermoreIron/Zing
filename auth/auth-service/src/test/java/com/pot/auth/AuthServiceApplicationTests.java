package com.pot.auth;

import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.WeChatPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring上下文集成启动测试
 *
 * <p>
 * 验证整个Spring Boot应用上下文能够正常启动，
 * 捕获Bean配置错误、循环依赖等上下文加载问题。
 *
 * <p>
 * 外部依赖通过以下方式隔离：
 * <ul>
 * <li>application-test.yml 禁用Nacos、限流、MQ、Touch等</li>
 * <li>@MockBean 替代需要网络连接的适配器</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@DisplayName("Spring上下文加载测试")
class AuthServiceApplicationTests {

    /**
     * 阻止TouchModuleAdapter被创建（需要真实的TouchService）
     */
    @MockitoBean
    private NotificationPort notificationPort;

    /**
     * 阻止MemberModuleAdapter被创建（需要Nacos + member-service）
     */
    @MockitoBean
    private UserModulePort userModulePort;

    /**
     * OAuth2OneStopAuthStrategy需要OAuth2Port，而HttpOAuth2PortAdapter需要auth.oauth2.enabled=true
     * 测试环境Mock替代
     */
    @MockitoBean
    private OAuth2Port oAuth2Port;

    /**
     * WeChatOneStopAuthStrategy需要WeChatPort，而HttpWeChatPortAdapter需要auth.wechat.enabled=true
     * 测试环境Mock替代
     */
    @MockitoBean
    private WeChatPort weChatPort;

    @Test
    @DisplayName("Spring上下文正常启动，无Bean配置错误")
    void contextLoads() {
        // 如果上下文加载失败，此测试自动失败
    }
}
