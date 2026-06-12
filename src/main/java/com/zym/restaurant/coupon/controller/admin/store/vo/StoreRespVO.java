package com.zym.restaurant.coupon.controller.admin.store.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreRespVO {

    private Long id;

    private String name;

    private String address;

    private String phone;

    private Integer status;

    private LocalDateTime createTime;
}
