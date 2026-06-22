package com.example.oa.common.constant;

public final class RabbitMqConstants {

    public static final String NOTIFICATION_EXCHANGE = "oa.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "oa.notification.queue.v2";
    public static final String NOTIFICATION_ROUTING_KEY = "oa.notification.*";
    public static final String NOTIFICATION_DEAD_EXCHANGE = "oa.notification.dead.exchange";
    public static final String NOTIFICATION_DEAD_QUEUE = "oa.notification.dead.queue";
    public static final String NOTIFICATION_DEAD_ROUTING_KEY = "oa.notification.dead";

    private RabbitMqConstants() {
    }
}
