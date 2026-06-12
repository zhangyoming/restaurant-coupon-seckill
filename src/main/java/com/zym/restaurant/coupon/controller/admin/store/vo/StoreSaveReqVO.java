package com.zym.restaurant.coupon.controller.admin.store.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreSaveReqVO {

    private Long id;

    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String address;

    private String phone;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
