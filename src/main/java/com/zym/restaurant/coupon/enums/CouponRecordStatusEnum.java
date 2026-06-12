package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponRecordStatusEnum {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer status;
    private final String name;
}
