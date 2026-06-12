package com.zym.restaurant.coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zym.restaurant.coupon.dal.mapper")
public class RestaurantCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantCouponApplication.class, args);
    }
}
