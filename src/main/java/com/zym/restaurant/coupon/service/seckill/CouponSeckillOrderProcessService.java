package com.zym.restaurant.coupon.service.seckill;

import com.zym.restaurant.coupon.mq.message.CouponSeckillMessage;

/**
 * 秒杀订单异步落库处理 Service。
 */
public interface CouponSeckillOrderProcessService {

    /**
     * 消费 MQ 消息，异步完成数据库扣库存、创建优惠券领取记录、更新抢券结果。
     */
    void processSeckillMessage(CouponSeckillMessage message);
}
