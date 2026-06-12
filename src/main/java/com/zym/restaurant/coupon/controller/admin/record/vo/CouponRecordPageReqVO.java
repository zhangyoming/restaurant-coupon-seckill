package com.zym.restaurant.coupon.controller.admin.record.vo;

import com.zym.restaurant.coupon.common.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CouponRecordPageReqVO extends PageParam {

    private Long userId;

    private Long activityId;

    private Long storeId;
}
