package com.seckill.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * Seckill System Application
 *
 * High Concurrency Seckill System with:
 * - Redis for stock caching and distributed locks
 * - RabbitMQ for async order processing
 * - MySQL for persistent storage
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.seckill", "config", "constant", "dto", "entity", "exception"})
@MapperScan("com.seckill.api.mapper")
public class SeckillApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context= SpringApplication.run(SeckillApplication.class, args);



        System.out.println("===========================================");
        System.out.println("   Seckill System Started Successfully!");
        System.out.println("   API Documentation: http://localhost:8080");
        System.out.println("===========================================");

//        context.publishEvent(new UserRegisteredEvent(context));
    }
}
