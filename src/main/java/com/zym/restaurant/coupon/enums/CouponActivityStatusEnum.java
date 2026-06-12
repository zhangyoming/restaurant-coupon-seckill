package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponActivityStatusEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    ENDED(2, "已结束"),
    OFFLINE(3, "已下架");

    private final Integer status;
    private final String name;
}
