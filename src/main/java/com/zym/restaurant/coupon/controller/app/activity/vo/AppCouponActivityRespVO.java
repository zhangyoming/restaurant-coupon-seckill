package com.zym.restaurant.coupon.controller.app.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AppCouponActivityRespVO {

    private Long id;

    private Long storeId;

    private String title;

    private String couponName;

    private BigDecimal couponAmount;

    private BigDecimal thresholdAmount;

    private Integer availableStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
