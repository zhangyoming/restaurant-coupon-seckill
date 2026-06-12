package com.zym.restaurant.coupon.controller.app.seckill.vo;

import lombok.Data;

@Data
public class CouponSeckillCaptchaRespVO {

    /** 验证码编号 */
    private String captchaId;

    /** 展示给用户的算术表达式，例如：3 + 5 = ? */
    private String expression;

    /** 有效期，单位：秒 */
    private Long expireSeconds;
}
