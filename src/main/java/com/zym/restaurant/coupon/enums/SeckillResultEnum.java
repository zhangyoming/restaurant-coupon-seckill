package com.zym.restaurant.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeckillResultEnum {

    NOT_REQUESTED("NOT_REQUESTED", "未发起抢券"),
    QUEUING("QUEUING", "排队中"),
    SUCCESS("SUCCESS", "领取成功"),
    FAILED("FAILED", "领取失败"),
    SOLD_OUT("SOLD_OUT", "优惠券已抢完"),
    DUPLICATE("DUPLICATE", "请勿重复领取"),
    NOT_STARTED("NOT_STARTED", "活动未开始"),
    ENDED("ENDED", "活动已结束");

    private final String code;
    private final String name;
}
