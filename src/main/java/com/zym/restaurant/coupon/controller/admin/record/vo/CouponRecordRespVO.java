package com.zym.restaurant.coupon.controller.admin.record.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRecordRespVO {

    private Long id;

    private Long userId;

    private Long activityId;

    private Long storeId;

    private String couponName;

    private BigDecimal couponAmount;

    private BigDecimal thresholdAmount;

    private Integer status;

    private LocalDateTime receiveTime;
}
