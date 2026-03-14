package com.pot.auth.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.authorization.event.PermissionChangedEvent;
import com.pot.auth.domain.authorization.listener.PermissionChangedListener;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PermissionChangedListener 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>合法消息时，正确调用 incrementPermissionVersion 和 invalidatePermissionCache</li>
 * <li>JSON解析异常时，不抛出异常（静默降级），不调用领域服务</li>
 * <li>PermissionDomainService 抛出异常时，异常被捕获，不向外传播</li>
 * </ul>
 *
 * <p>
 * 使用 {@code @Mock ObjectMapper} 并通过 {@code when(objectMapper.readValue(...))}
 * 控制解析结果，
 * 避免依赖 {@code PermissionChangedEvent} 的 JSON 反序列化能力（Lombok {@code @Builder}
 * 无默认构造器）。
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionChangedListener 单元测试")
class PermissionChangedListenerTest {

    @Mock
    private PermissionDomainService permissionDomainService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PermissionChangedListener listener;

    private PermissionChangedEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = PermissionChangedEvent.builder()
                .userId("10001")
                .namespace("member")
                .changeType(PermissionChangedEvent.ChangeType.PERMISSION_CHANGED)
                .reason("管理员手动修改权限")
                .build();
    }

    @Test
    @DisplayName("合法消息：调用 incrementPermissionVersion 和 invalidatePermissionCache")
    void whenValidMessage_thenTriggerVersionIncrementAndCacheInvalidation() throws Exception {
        // given
        String message = "any-message";
        when(objectMapper.readValue(eq(message), eq(PermissionChangedEvent.class)))
                .thenReturn(validEvent);

        // when
        listener.handlePermissionChanged(message);

        // then
        verify(permissionDomainService).incrementPermissionVersion("member", "10001");
        verify(permissionDomainService).invalidatePermissionCache("member", "10001");
    }

    @Test
    @DisplayName("JSON解析抛出异常：不抛出异常，不调用领域服务")
    void whenJsonParseThrows_thenNoException() throws Exception {
        // given
        String invalidJson = "not-valid";
        when(objectMapper.readValue(eq(invalidJson), eq(PermissionChangedEvent.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "parse error"));

        // when & then: 不应抛出异常
        listener.handlePermissionChanged(invalidJson);

        // then: 领域服务不被调用
        verifyNoInteractions(permissionDomainService);
    }

    @Test
    @DisplayName("PermissionDomainService 抛出异常：异常被捕获，不向外传播")
    void whenDomainServiceThrows_thenExceptionCaught() throws Exception {
        // given
        String message = "any-message-2";
        when(objectMapper.readValue(eq(message), eq(PermissionChangedEvent.class)))
                .thenReturn(validEvent);
        doThrow(new RuntimeException("Redis unavailable"))
                .when(permissionDomainService)
                .incrementPermissionVersion(anyString(), anyString());

        // when & then: 不抛出异常
        listener.handlePermissionChanged(message);
    }
}
