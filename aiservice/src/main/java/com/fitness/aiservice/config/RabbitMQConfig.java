package com.fitness.aiservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Value("${rabbitmq.queue.name}")
    private String queue;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Bean
    public Queue activityQueue() {
        // This declares a queue name
        return new Queue(queue, true);
    }

    @Bean
    public DirectExchange activityExchange() {
        // Getting exchange
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, DirectExchange activityExchange) {
        // Binding to routing key
        return BindingBuilder.bind(activityQueue).to(activityExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // This converts Java object into Json before sending to queue.
         return new Jackson2JsonMessageConverter();
    }
}
