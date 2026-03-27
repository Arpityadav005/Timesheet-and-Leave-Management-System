package com.tms.ts.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "timesheet.exchange";
    public static final String TIMESHEET_QUEUE = "timesheet.submitted";
    public static final String TIMESHEET_ROUTING_KEY = "timesheet.submitted";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue timesheetQueue() {
        return new Queue(TIMESHEET_QUEUE, true);
    }

    @Bean
    public Binding timesheetBinding() {
        return BindingBuilder
                .bind(timesheetQueue())
                .to(exchange())
                .with(TIMESHEET_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}