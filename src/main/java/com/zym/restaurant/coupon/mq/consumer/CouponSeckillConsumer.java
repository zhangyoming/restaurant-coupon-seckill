package com.zym.restaurant.coupon.mq.consumer;

import com.zym.restaurant.coupon.mq.config.CouponRabbitMqConfig;
import com.zym.restaurant.coupon.mq.message.CouponSeckillMessage;
import com.zym.restaurant.coupon.service.seckill.CouponSeckillOrderProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券秒杀消息消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponSeckillConsumer {

    private final CouponSeckillOrderProcessService orderProcessService;

    @RabbitListener(queues = CouponRabbitMqConfig.SECKILL_QUEUE)
    public void onMessage(CouponSeckillMessage message) {
        log.info("收到优惠券秒杀消息：{}", message);
        orderProcessService.processSeckillMessage(message);
    }
}
