package com.zym.restaurant.coupon.controller.admin.activity.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponActivitySaveReqVO {

    private Long id;

    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotBlank(message = "优惠券名称不能为空")
    private String couponName;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.01", message = "优惠金额必须大于 0")
    private BigDecimal couponAmount;

    @DecimalMin(value = "0.00", message = "使用门槛不能小于 0")
    private BigDecimal thresholdAmount;

    @NotNull(message = "总库存不能为空")
    @Min(value = 1, message = "总库存必须大于 0")
    private Integer totalStock;

    @NotNull(message = "每人限领数量不能为空")
    @Min(value = 1, message = "每人限领数量至少为 1")
    private Integer perUserLimit;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
