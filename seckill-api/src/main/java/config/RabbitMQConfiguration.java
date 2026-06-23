package config;

import constant.SeckillConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * Declares exchange, queue, and bindings for the seckill system.
 * Spring Boot自动配置会:
 * 1. 从application.yml读取spring.rabbitmq配置创建CachingConnectionFactory
 * 2. 自动创建RabbitAdmin (autoStartup=true)，在容器启动后声明所有Exchange/Queue/Binding
 * 这里只需要声明Exchange/Queue/Binding Bean即可，无需手动操作RabbitAdmin
 */
@Configuration
public class RabbitMQConfiguration {

    /**
     * Topic exchange for seckill messages
     */
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SeckillConstant.SECKILL_EXCHANGE, true, false);
    }

    /**
     * Main seckill queue — 持久化队列
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
