package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";
    public static final String ORDER_CONFIRMED_KEY = "order.confirmed";
    public static final String ORDER_FAILED_KEY = "order.failed";

    public static final String ORDER_CONFIRMED_QUEUE = "order-service.order.confirmed";
    public static final String ORDER_FAILED_QUEUE    = "order-service.order.failed";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Queue orderFailedQueue() {
        return QueueBuilder.durable(ORDER_FAILED_QUEUE).build();
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(ordersExchange).with(ORDER_CONFIRMED_KEY);
    }

    @Bean
    public Binding orderFailedBinding(Queue orderFailedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderFailedQueue).to(ordersExchange).with(ORDER_FAILED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
