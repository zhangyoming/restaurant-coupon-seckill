package com.zym.restaurant.coupon.controller.app.seckill.vo;

import lombok.Data;

@Data
public class CouponSeckillRespVO {

    private String requestId;

    private Long orderId;

    /** QUEUING / SUCCESS / SOLD_OUT / DUPLICATE / NOT_STARTED / ENDED */
    private String resultCode;

    private String resultMsg;
}
