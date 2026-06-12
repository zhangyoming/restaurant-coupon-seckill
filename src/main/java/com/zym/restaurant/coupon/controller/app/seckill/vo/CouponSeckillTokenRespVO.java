package com.zym.restaurant.coupon.controller.app.seckill.vo;

import lombok.Data;

@Data
public class CouponSeckillTokenRespVO {

    /** 秒杀令牌，抢券时需要提交 */
    private String token;

    /** 有效期，单位：秒 */
    private Long expireSeconds;
}
