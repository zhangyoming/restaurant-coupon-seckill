package com.zym.restaurant.coupon.controller.app.seckill.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponSeckillReqVO {

    /** 第 5 阶段仍用 userId 模拟登录用户；后续可替换为 JWT / Sa-Token 登录态 */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @NotNull(message = "活动编号不能为空")
    private Long activityId;

    /** 秒杀令牌：先通过 /token 接口获取，再请求真正的抢券接口；本地压测可通过配置临时关闭令牌校验 */
    private String token;
}
