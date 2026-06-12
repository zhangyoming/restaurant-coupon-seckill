package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponSeckillOrderStatusEnum {

    QUEUING(0, "排队中"),
    SUCCESS(1, "领取成功"),
    FAIL(2, "领取失败");

    private final Integer status;
    private final String name;
}
