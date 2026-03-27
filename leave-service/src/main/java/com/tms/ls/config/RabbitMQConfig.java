package com.tms.ls.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "leave.exchange";
    public static final String LEAVE_QUEUE = "leave.requested";
    public static final String LEAVE_ROUTING_KEY = "leave.requested";

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue leaveQueue() {
        return new Queue(LEAVE_QUEUE, true);
    }

    @Bean
    public Binding leaveBinding() {
        return BindingBuilder
                .bind(leaveQueue())
                .to(leaveExchange())
                .with(LEAVE_ROUTING_KEY);
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