package com.zym.restaurant.coupon.controller.app.seckill.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponSeckillTokenCreateReqVO {

    /** 第 5 阶段仍用 userId 模拟登录用户；后续可替换为 JWT / Sa-Token 登录态 */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @NotNull(message = "活动编号不能为空")
    private Long activityId;

    @NotBlank(message = "验证码编号不能为空")
    private String captchaId;

    @NotBlank(message = "验证码答案不能为空")
    private String captchaCode;
}
