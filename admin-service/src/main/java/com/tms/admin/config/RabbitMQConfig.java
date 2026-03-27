package com.tms.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    @Bean
    public TopicExchange timesheetExchange() {
        return new TopicExchange("timesheet.exchange");
    }

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange("leave.exchange");
    }
    
    @Bean
    public TopicExchange adminExchange() {
        return new TopicExchange("admin.exchange");
    }

    // Queues
    @Bean
    public Queue adminTimesheetQueue() {
        return new Queue("admin.timesheet.queue");
    }

    @Bean
    public Queue adminLeaveQueue() {
        return new Queue("admin.leave.queue");
    }

    // Bindings
    @Bean
    public Binding bindingTimesheetToAdmin(
            @org.springframework.beans.factory.annotation.Qualifier("adminTimesheetQueue") Queue adminTimesheetQueue, 
            @org.springframework.beans.factory.annotation.Qualifier("timesheetExchange") TopicExchange timesheetExchange) {
        return BindingBuilder.bind(adminTimesheetQueue).to(timesheetExchange).with("timesheet.submitted");
    }

    @Bean
    public Binding bindingLeaveToAdmin(
            @org.springframework.beans.factory.annotation.Qualifier("adminLeaveQueue") Queue adminLeaveQueue, 
            @org.springframework.beans.factory.annotation.Qualifier("leaveExchange") TopicExchange leaveExchange) {
        return BindingBuilder.bind(adminLeaveQueue).to(leaveExchange).with("leave.requested");
    }

    // Converters
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
