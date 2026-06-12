package com.zym.restaurant.coupon.controller.app.seckill.vo;

import lombok.Data;

@Data
public class CouponSeckillResultRespVO {

    private Long activityId;

    private Long userId;

    private String requestId;

    private Integer orderStatus;

    private String resultCode;

    private String resultMsg;

    private String failReason;
}
