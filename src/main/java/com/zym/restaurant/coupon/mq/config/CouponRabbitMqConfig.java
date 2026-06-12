package com.zym.restaurant.coupon.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优惠券秒杀 RabbitMQ 配置。
 *
 * 第 4 阶段使用 RabbitMQ 异步发券：
 * 1. 抢券接口只完成 Redis Lua 预扣库存和创建排队订单；
 * 2. 事务提交后投递 MQ 消息；
 * 3. 消费者异步扣减数据库库存、创建领取记录、更新秒杀结果。
 */
@Configuration
public class CouponRabbitMqConfig {

    public static final String SECKILL_EXCHANGE = "coupon.seckill.exchange";

    public static final String SECKILL_QUEUE = "coupon.seckill.queue";

    public static final String SECKILL_ROUTING_KEY = "coupon.seckill.create";

    @Bean
    public DirectExchange couponSeckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue couponSeckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    @Bean
    public Binding couponSeckillBinding(Queue couponSeckillQueue, DirectExchange couponSeckillExchange) {
        return BindingBuilder.bind(couponSeckillQueue)
                .to(couponSeckillExchange)
                .with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(20);
        return factory;
    }
}
