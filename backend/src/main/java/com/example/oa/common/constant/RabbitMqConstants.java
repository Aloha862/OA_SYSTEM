package com.example.oa.common.constant;

public final class RabbitMqConstants {

    public static final String NOTIFICATION_EXCHANGE = "oa.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "oa.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "oa.notification.*";

    private RabbitMqConstants() {
    }
}
