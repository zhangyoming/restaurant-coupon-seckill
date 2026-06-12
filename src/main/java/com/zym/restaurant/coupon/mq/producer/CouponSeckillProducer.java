package com.zym.restaurant.coupon.mq.producer;

import com.zym.restaurant.coupon.mq.config.CouponRabbitMqConfig;
import com.zym.restaurant.coupon.mq.message.CouponSeckillMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 优惠券秒杀消息生产者。
 */
@Component
@RequiredArgsConstructor
public class CouponSeckillProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSeckillMessage(CouponSeckillMessage message) {
        rabbitTemplate.convertAndSend(
                CouponRabbitMqConfig.SECKILL_EXCHANGE,
                CouponRabbitMqConfig.SECKILL_ROUTING_KEY,
                message
        );
    }
}
