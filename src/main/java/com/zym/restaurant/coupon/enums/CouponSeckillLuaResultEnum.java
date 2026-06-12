package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Redis Lua 抢券脚本返回结果。
 */
@Getter
@AllArgsConstructor
public enum CouponSeckillLuaResultEnum {

    SUCCESS(0L, "预扣库存成功"),
    SOLD_OUT(1L, "优惠券已抢完"),
    DUPLICATE(2L, "请勿重复领取"),
    CACHE_NOT_FOUND(3L, "活动库存未预热");

    private final Long code;
    private final String name;

    public static CouponSeckillLuaResultEnum of(Long code) {
        return Arrays.stream(values())
                .filter(item -> item.getCode().equals(code))
                .findFirst()
                .orElse(CACHE_NOT_FOUND);
    }
}
