package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatusEnum {

    ENABLE(0, "启用"),
    DISABLE(1, "禁用");

    private final Integer status;
    private final String name;
}
