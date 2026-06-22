package com.example.oa.config;

import com.example.oa.common.constant.RabbitMqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitMqConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(RabbitMqConstants.NOTIFICATION_QUEUE)
                .deadLetterExchange(RabbitMqConstants.NOTIFICATION_DEAD_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstants.NOTIFICATION_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(RabbitMqConstants.NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public TopicExchange notificationDeadExchange() {
        return new TopicExchange(RabbitMqConstants.NOTIFICATION_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationDeadQueue() {
        return QueueBuilder.durable(RabbitMqConstants.NOTIFICATION_DEAD_QUEUE).build();
    }

    @Bean
    public Binding notificationDeadBinding() {
        return BindingBuilder.bind(notificationDeadQueue())
                .to(notificationDeadExchange())
                .with(RabbitMqConstants.NOTIFICATION_DEAD_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ消息确认失败: correlationData={}, cause={}", correlationData, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned ->
                log.warn("RabbitMQ消息路由失败: exchange={}, routingKey={}, body={}",
                        returned.getExchange(), returned.getRoutingKey(), new String(returned.getMessage().getBody())));
        return rabbitTemplate;
    }
}
