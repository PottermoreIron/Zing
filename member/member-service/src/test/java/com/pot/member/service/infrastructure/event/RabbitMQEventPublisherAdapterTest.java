package com.pot.member.service.infrastructure.event;

import com.pot.member.service.domain.event.MemberDomainEvent;
import com.pot.zing.framework.mq.core.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQEventPublisherAdapter")
class RabbitMQEventPublisherAdapterTest {

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private RabbitMQEventPublisherAdapter adapter;

        private MemberDomainEvent testEvent(String aggregateId) {
        return new MemberDomainEvent(aggregateId) {
            @Override
            protected String getEventName() {
                return "test.happened";
            }
        };
    }

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("正常发布：调用 MessageProducer.send 并传入正确的 topic/routingKey/event")
        void publish_delegatesToMessageProducer() {
            MemberDomainEvent event = testEvent("member-42");

            adapter.publish(event);

            then(messageProducer).should().send(
                    eq("member.events"), // topic = {domain}.events
                    eq("member.test.happened.v1"), // routingKey = {domain}.{event}.{version}
                    eq(event));
        }

        @Test
        @DisplayName("MessageProducer 抛出异常 → 包裹为 RuntimeException 并重新抛出")
        void publish_producerThrows_wrapsInRuntimeException() {
            MemberDomainEvent event = testEvent("member-99");
            willThrow(new RuntimeException("RabbitMQ down"))
                    .given(messageProducer).send(any(), any(), any());

            assertThatThrownBy(() -> adapter.publish(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("领域事件发布失败");
        }
    }
}
