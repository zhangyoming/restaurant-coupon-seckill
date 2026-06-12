package com.zym.restaurant.coupon.controller.admin.activity.vo;

import com.zym.restaurant.coupon.common.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CouponActivityPageReqVO extends PageParam {

    private Long storeId;

    private String title;

    private Integer status;
}
