package com.example.oa.module.notification;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.module.notification.service.NotificationOutboxService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationProducerTest {
    @Test
    void sendPersistsToOutboxInsteadOfDependingOnBrokerAvailability() {
        NotificationOutboxService outbox = mock(NotificationOutboxService.class);
        NotificationProducer producer = new NotificationProducer(outbox);
        NotificationMessage message = new NotificationMessage();
        message.setEventId("event-1");

        producer.send("oa.notification.system", message);

        verify(outbox).enqueue("oa.notification.system", message);
    }
}
