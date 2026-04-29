package com.ecommerce.aggregator.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Configuration
public class AnalyticsRabbitConfig {

    public static final String USER_ACTIVITY_EXCHANGE = "user-activity.exchange";
    public static final String SEARCH_ROUTING_KEY = "user.search";
    public static final String VIEW_ROUTING_KEY = "user.product.viewed";
    public static final String FEEDBACK_ROUTING_KEY = "recommendation.feedback";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
