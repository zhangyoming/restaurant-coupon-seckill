package com.zym.restaurant.coupon.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券秒杀异步发券消息。
 */
@Data
public class CouponSeckillMessage implements Serializable {

    private Long userId;

    private Long activityId;

    private Long storeId;

    private Long orderId;

    /** 请求编号，用于查询结果和幂等识别 */
    private String requestId;

    private LocalDateTime requestTime;
}
