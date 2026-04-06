package com.pot.auth.domain;

import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.infrastructure.event.PermissionChangedEvent;
import com.pot.auth.infrastructure.listener.PermissionChangedEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for PermissionChangedEventListener.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionChangedEventListener 单元测试")
class PermissionChangedListenerTest {

    @Mock
    private PermissionDomainService permissionDomainService;

    @InjectMocks
    private PermissionChangedEventListener listener;

    private PermissionChangedEvent validEvent;

    @BeforeEach
    void setUp() throws Exception {
        validEvent = new PermissionChangedEvent();
        setField(validEvent, "affectedMemberIds", Set.of(10001L, 10002L));
        setField(validEvent, "changeType", PermissionChangedEvent.ChangeType.ROLE_UPDATED);
        setField(validEvent, "roleId", 20001L);
        setField(validEvent, "permissionId", 30001L);
        setField(validEvent, "reason", "管理员手动修改权限");
        setField(validEvent, "eventId", "evt-10001");
    }

    @Nested
    @DisplayName("consume()")
    class Consume {

        @Test
        @DisplayName("合法事件：对所有受影响会员失效权限缓存")
        void whenValidEvent_thenInvalidateAllAffectedMemberCaches() {
            listener.consume(validEvent);

            verify(permissionDomainService, times(2))
                    .invalidatePermissionCache(any(UserId.class), any(UserDomain.class));
            verify(permissionDomainService)
                    .invalidatePermissionCache(new UserId(10001L), UserDomain.MEMBER);
            verify(permissionDomainService)
                    .invalidatePermissionCache(new UserId(10002L), UserDomain.MEMBER);
        }

        @Test
        @DisplayName("领域服务抛出异常：异常被捕获，不向外传播")
        void whenDomainServiceThrows_thenSwallowException() {
            doThrow(new RuntimeException("Redis unavailable"))
                    .when(permissionDomainService)
                    .invalidatePermissionCache(any(UserId.class), eq(UserDomain.MEMBER));

            listener.consume(validEvent);

            verify(permissionDomainService)
                    .invalidatePermissionCache(any(UserId.class), eq(UserDomain.MEMBER));
        }
    }

    @Test
    @DisplayName("MQ 元数据：返回正确的消息类型和队列名")
    void shouldExposeQueueMetadata() {
        assertThat(listener.getMessageType()).isEqualTo(PermissionChangedEvent.class);
        assertThat(listener.getQueue()).isEqualTo("member.permission");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
