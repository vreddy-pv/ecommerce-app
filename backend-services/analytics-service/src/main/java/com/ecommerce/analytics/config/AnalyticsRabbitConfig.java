package com.ecommerce.analytics.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsRabbitConfig {

    public static final String SEARCH_QUEUE = "user-activity.search.queue";
    public static final String VIEW_QUEUE = "user-activity.view.queue";
    public static final String FEEDBACK_QUEUE = "user-activity.feedback.queue";

    @Bean
    Queue searchQueue() {
        return new Queue(SEARCH_QUEUE, true);
    }

    @Bean
    Queue viewQueue() {
        return new Queue(VIEW_QUEUE, true);
    }

    @Bean
    Queue feedbackQueue() {
        return new Queue(FEEDBACK_QUEUE, true);
    }
}
