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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@DisplayName("Spring context load test")
class AuthServiceApplicationTests {

    // Avoid creating TouchModuleAdapter during context startup.
    @MockitoBean
    private NotificationPort notificationPort;

    // Avoid creating MemberModuleAdapter during context startup.
    @MockitoBean
    private UserModulePort userModulePort;

    // Provide OAuth2Port without enabling the HTTP adapter in the test profile.
    @MockitoBean
    private OAuth2Port oAuth2Port;

    // Provide WeChatPort without enabling the HTTP adapter in the test profile.
    @MockitoBean
    private WeChatPort weChatPort;

    @Test
    @DisplayName("Spring context starts normally with no bean configuration errors")
    void contextLoads() {
    }
}
