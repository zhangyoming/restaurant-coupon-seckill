package com.zym.restaurant.coupon.controller.admin.store.vo;

import com.zym.restaurant.coupon.common.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StorePageReqVO extends PageParam {

    private String name;

    private Integer status;
}
