package com.seckill.api.config;

import com.seckill.api.constant.SeckillConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * Declares exchange, queue, and bindings for the seckill system
 */
@Configuration
public class RabbitMQConfiguration {

    /**
     * Topic exchange for seckill messages
     */
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SeckillConstant.SECKILL_EXCHANGE);
    }

    /**
     * Main seckill queue
     */
    @Bean
    public Queue seckillQueue() {
        return new Queue(SeckillConstant.SECKILL_QUEUE, true);
    }

    /**
     * Bind queue to exchange with routing key
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder
                .bind(seckillQueue())
                .to(seckillExchange())
                .with(SeckillConstant.SECKILL_ROUTING_KEY);
    }
}
