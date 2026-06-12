package com.zym.restaurant.coupon.controller.admin.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponActivityRespVO {

    private Long id;

    private Long storeId;

    private String title;

    private String couponName;

    private BigDecimal couponAmount;

    private BigDecimal thresholdAmount;

    private Integer totalStock;

    private Integer availableStock;

    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime createTime;
}
